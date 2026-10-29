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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
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
        // Edge-to-edge is enforced on new Android versions: keep the web page (and its top bar)
        // below the status bar (clock / notification icons) and above the nav bar / keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
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
            // Bundled videos are served with HTTP Range support so they can seek, loop and stream smoothly.
            serveMedia(request)?.let { return it }
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

    /**
     * Serves files under assets/media/ (videos, posters) with byte-range support.
     * WebViewAssetLoader always answers 200 with the whole file, which makes <video> seeking and
     * looping slow for large films; this answers "Range: bytes=a-b" with 206 Partial Content.
     * Media files are stored uncompressed in the APK (see androidResources.noCompress), so they
     * can be opened with openFd(). Returns null to fall back to the normal asset loader.
     */
    private fun serveMedia(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        if (!isAppOrigin(uri) || !request.method.equals("GET", true)) return null
        val path = uri.path ?: return null
        if (!path.startsWith(MEDIA_PREFIX) || path.contains("..")) return null
        val assetPath = path.removePrefix("/assets/")
        val afd = try {
            assets.openFd(assetPath)
        } catch (e: IOException) {
            return null
        }
        val total = afd.length
        if (total <= 0L) {
            afd.close()
            return null
        }
        val mime = when (assetPath.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
        var start = 0L
        var end = total - 1
        var partial = false
        val rangeHeader = request.requestHeaders.entries.firstOrNull { it.key.equals("Range", true) }?.value
        val match = rangeHeader?.let { RANGE_RE.matchEntire(it.trim()) }
        if (match != null) {
            val (a, b) = match.destructured
            when {
                a.isEmpty() && b.isEmpty() -> Unit
                a.isEmpty() -> {
                    val suffix = b.toLongOrNull() ?: 0L
                    start = (total - suffix).coerceAtLeast(0L)
                    partial = true
                }
                else -> {
                    start = a.toLongOrNull() ?: Long.MAX_VALUE
                    end = (if (b.isEmpty()) total - 1 else (b.toLongOrNull() ?: (total - 1))).coerceAtMost(total - 1)
                    partial = true
                }
            }
            if (partial && (start >= total || start > end)) {
                afd.close()
                return WebResourceResponse(
                    mime, null, 416, "Range Not Satisfiable",
                    mapOf("Content-Range" to "bytes */$total", "Accept-Ranges" to "bytes"),
                    ByteArrayInputStream(ByteArray(0))
                )
            }
        }
        val length = end - start + 1
        val stream: InputStream = try {
            val raw = afd.createInputStream()
            var skipped = 0L
            while (skipped < start) {
                val n = raw.skip(start - skipped)
                if (n <= 0L) break
                skipped += n
            }
            BoundedStream(raw, length)
        } catch (e: IOException) {
            afd.close()
            return null
        }
        val headers = linkedMapOf(
            "Accept-Ranges" to "bytes",
            "Content-Length" to length.toString(),
            "Cache-Control" to "max-age=86400"
        )
        if (partial) headers["Content-Range"] = "bytes $start-$end/$total"
        return WebResourceResponse(
            mime, null,
            if (partial) 206 else 200,
            if (partial) "Partial Content" else "OK",
            headers, stream
        )
    }

    /** Reads at most [limit] bytes, then reports end-of-stream; closing it closes the asset. */
    private class BoundedStream(input: InputStream, private var limit: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (limit <= 0L) return -1
            val r = super.read()
            if (r >= 0) limit--
            return r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (limit <= 0L) return -1
            val r = super.read(b, off, minOf(len.toLong(), limit).toInt())
            if (r > 0) limit -= r
            return r
        }

        override fun skip(n: Long): Long {
            val r = super.skip(minOf(n, limit))
            if (r > 0) limit -= r
            return r
        }

        override fun available(): Int = minOf(super.available().toLong(), limit).toInt()
        override fun markSupported(): Boolean = false
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
        private const val MEDIA_PREFIX = "/assets/media/"
        private val RANGE_RE = Regex("""bytes=(\d*)-(\d*)""", RegexOption.IGNORE_CASE)
        private const val WEB_STATE = "matix_webview_state"
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
