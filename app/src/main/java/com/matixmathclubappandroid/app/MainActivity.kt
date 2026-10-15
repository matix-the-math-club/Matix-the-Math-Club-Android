package com.matixmathclubappandroid.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.util.Locale

/** Native Android host for the original Matix HTML UI, with documented runtime guards. */
class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private val popups = linkedMapOf<WebView, Dialog>()
    private val jsDialogs = mutableSetOf<AlertDialog>()
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var filePickerInFlight = false
    private var pendingPermission: PermissionRequest? = null
    private var permissionPromptInFlight = false
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var loadErrorDialog: AlertDialog? = null

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = uploadCallback
        uploadCallback = null
        filePickerInFlight = false
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback?.onReceiveValue(uris)
    }

    private val mediaPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionPromptInFlight = false
        val request = pendingPermission
        pendingPermission = null
        if (request != null) finishMediaPermissionRequest(request)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        webView = createWebView()
        root.addView(webView, FrameLayout.LayoutParams(MATCH, MATCH))
        val restored = savedInstanceState?.getBundle(WEB_STATE)?.let { webView.restoreState(it) }
        if (restored == null) webView.loadUrl(APP_URL)

        onBackPressedDispatcher.addCallback(this) {
            when {
                fullscreenView != null -> hideFullscreen()
                popups.isNotEmpty() -> popups.values.last().dismiss()
                webView.canGoBack() -> webView.goBack()
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled") // The supplied application is written in JavaScript.
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.BLACK)
        overScrollMode = View.OVER_SCROLL_NEVER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isForceDarkAllowed = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true // User-selected document-picker URIs only; no JS bridge is exposed.
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, false)
            }
        }
        CookieManager.getInstance().setAcceptCookie(true)
        // Embedded provider content may need cookies; origin-scoped storage is still handled by WebView.
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = MatixWebViewClient()
        webChromeClient = MatixChromeClient()
        setDownloadListener { url, _, _, _, _ ->
            // Let the user's browser handle ordinary remote downloads without broad storage permission.
            if (!openExternal(Uri.parse(url))) {
                toast("This download needs to be opened in a browser.")
            }
        }
    }

    private inner class MatixWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val response = assetLoader.shouldInterceptRequest(request.url)
            if (response != null) return response
            // Missing files on the reserved app origin must never fall through to the public network.
            if (isAppOrigin(request.url)) {
                return WebResourceResponse(
                    "text/plain", "UTF-8", 404, "Not Found",
                    mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0))
                )
            }
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val uri = request.url
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            if (isAppOrigin(uri) || scheme == "about" || scheme == "blob" || scheme == "data") return false
            val opened = openExternal(uri)
            if (opened) popups[view]?.dismiss()
            return true // Never navigate the trusted app window to an unrelated site or local file.
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel() // Never bypass invalid server certificates.
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame && view === webView && isAppOrigin(request.url)) {
                showLoadError()
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (request.isForMainFrame && view === webView && isAppOrigin(request.url)) showLoadError()
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (view === webView) {
                hideFullscreen()
                popups.values.toList().forEach { it.dismiss() }
                root.removeView(view)
                view.destroy()
                webView = createWebView()
                root.addView(webView, 0, FrameLayout.LayoutParams(MATCH, MATCH))
                webView.loadUrl(APP_URL)
                toast("Matix restarted after the web renderer stopped.")
            } else {
                popups[view]?.dismiss()
            }
            return true
        }
    }

    private inner class MatixChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            // Do not let a second JS request steal the result from an already open document picker.
            if (filePickerInFlight) {
                callback.onReceiveValue(null)
                return true
            }
            uploadCallback = callback
            val types = params.acceptTypes.flatMap { it.split(',') }.mapNotNull { normalizeMime(it) }.distinct()
            val action = if (params.mode == FileChooserParams.MODE_SAVE) Intent.ACTION_CREATE_DOCUMENT else Intent.ACTION_OPEN_DOCUMENT
            val intent = Intent(action).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = types.singleOrNull() ?: "*/*"
                if (types.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                if (action == Intent.ACTION_CREATE_DOCUMENT) putExtra(Intent.EXTRA_TITLE, params.filenameHint ?: "matix-file")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                filePickerInFlight = true
                filePicker.launch(intent)
            } catch (_: ActivityNotFoundException) {
                filePickerInFlight = false
                uploadCallback = null
                callback.onReceiveValue(null)
                toast("No document picker is installed on this device.")
            }
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            runOnUiThread {
                if (!isAppOrigin(request.origin) || isFinishing || isDestroyed || permissionPromptInFlight) {
                    request.deny()
                    return@runOnUiThread
                }
                val supported = request.resources.filter { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE || it == PermissionRequest.RESOURCE_VIDEO_CAPTURE }
                if (supported.isEmpty()) {
                    request.deny()
                    return@runOnUiThread
                }
                val needed = supported.map(::androidPermission).distinct().filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (needed.isEmpty()) {
                    finishMediaPermissionRequest(request)
                } else {
                    pendingPermission = request
                    permissionPromptInFlight = true
                    mediaPermissions.launch(needed.toTypedArray())
                }
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            runOnUiThread { if (pendingPermission === request) pendingPermission = null }
        }

        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            if (!isUserGesture || popups.size >= 3 || isFinishing || isDestroyed) return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val child = createWebView()
            val dialog = Dialog(this@MainActivity)
            val column = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            val close = Button(this@MainActivity).apply {
                text = "Close window"
                setOnClickListener { dialog.dismiss() }
            }
            column.addView(close, LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
            column.addView(child, LinearLayout.LayoutParams(MATCH, 0, 1f))
            dialog.setContentView(column)
            dialog.setOnDismissListener {
                popups.remove(child)
                child.stopLoading()
                column.removeView(child)
                child.destroy()
            }
            popups[child] = dialog
            dialog.show()
            dialog.window?.setLayout(MATCH, MATCH)
            transport.webView = child
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) { popups[window]?.dismiss() }

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            showJsDialog(AlertDialog.Builder(this@MainActivity)
                .setTitle("Matix").setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                .setOnCancelListener { result.cancel() }.create())
            return true
        }

        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
            confirmJavaScript(message, result)
            return true
        }

        override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
            confirmJavaScript(message, result)
            return true
        }

        override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
            val input = EditText(this@MainActivity).apply { setText(defaultValue) }
            showJsDialog(AlertDialog.Builder(this@MainActivity)
                .setTitle("Matix").setMessage(message).setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm(input.text.toString()) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                .setOnCancelListener { result.cancel() }.create())
            return true
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (fullscreenView != null) { callback.onCustomViewHidden(); return }
            fullscreenView = view
            fullscreenCallback = callback
            view.setBackgroundColor(Color.BLACK)
            root.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
            WindowInsetsControllerCompat(window, root).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        override fun onHideCustomView() = hideFullscreen()
    }

    private fun confirmJavaScript(message: String, result: JsResult) {
        showJsDialog(AlertDialog.Builder(this)
            .setTitle("Matix").setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
            .setOnCancelListener { result.cancel() }.create())
    }

    private fun showJsDialog(dialog: AlertDialog) {
        if (isFinishing || isDestroyed) { dialog.cancel(); return }
        jsDialogs.add(dialog)
        dialog.setOnDismissListener { jsDialogs.remove(dialog) }
        dialog.show()
    }

    private fun finishMediaPermissionRequest(request: PermissionRequest) {
        val granted = request.resources.filter {
            (it == PermissionRequest.RESOURCE_AUDIO_CAPTURE || it == PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                ContextCompat.checkSelfPermission(this, androidPermission(it)) == PackageManager.PERMISSION_GRANTED
        }
        if (granted.isEmpty()) request.deny() else request.grant(granted.toTypedArray())
    }

    private fun androidPermission(resource: String): String = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
        else -> Manifest.permission.RECORD_AUDIO
    }

    private fun normalizeMime(value: String): String? {
        val trimmed = value.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith('.')) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(trimmed.drop(1))
        } else trimmed.takeIf { it.contains('/') }
    }

    private fun isAppOrigin(uri: Uri): Boolean =
        uri.scheme.equals("https", true) && uri.host.equals(APP_HOST, true) && (uri.port == -1 || uri.port == 443)

    private fun openExternal(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme == "intent") {
            val parsed = try { Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) } catch (_: Exception) { return false }
            val fallback = parsed.getStringExtra("browser_fallback_url")?.let(Uri::parse) ?: return false
            if (fallback.scheme !in setOf("https", "http")) return false
            return openExternal(fallback)
        }
        if (scheme !in setOf("https", "http", "mailto", "tel", "sms", "geo", "market")) return false
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
            true
        } catch (_: ActivityNotFoundException) {
            toast("No app is available to open this link.")
            false
        } catch (_: SecurityException) {
            toast("Android blocked this link.")
            false
        }
    }

    private fun showLoadError() {
        if (isFinishing || isDestroyed || loadErrorDialog?.isShowing == true) return
        loadErrorDialog = AlertDialog.Builder(this)
            .setTitle("Matix could not load")
            .setMessage("The bundled app page could not be loaded. Retry, or update Android System WebView and restart the app.")
            .setPositiveButton("Retry") { _, _ -> webView.loadUrl(APP_URL) }
            .setNegativeButton(android.R.string.cancel, null)
            .create().also { it.show() }
    }

    private fun hideFullscreen() {
        fullscreenView?.let { root.removeView(it) }
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        WindowInsetsControllerCompat(window, root).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onSaveInstanceState(outState: Bundle) {
        val webState = Bundle()
        webView.saveState(webState)
        outState.putBundle(WEB_STATE, webState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
        popups.keys.forEach { it.onResume() }
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        popups.keys.forEach { it.onPause() }
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        uploadCallback?.onReceiveValue(null)
        uploadCallback = null
        pendingPermission?.deny()
        pendingPermission = null
        jsDialogs.toList().forEach { it.cancel() }
        loadErrorDialog?.dismiss()
        if (::root.isInitialized) hideFullscreen()
        popups.values.toList().forEach { it.dismiss() }
        if (::webView.isInitialized) {
            root.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val APP_HOST = "appassets.androidplatform.net"
        private const val APP_URL = "https://$APP_HOST/assets/app.html"
        private const val WEB_STATE = "matix_webview_state"
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
