package club.matix.mathclub

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature

/**
 * Hosts the Matix web app in a WebView.
 *
 * The web app is a single file, `app.html`, which lives at the PROJECT ROOT.
 * The `syncWebApp` Gradle task copies it to `app/src/main/assets/app.html`
 * before each build, and [WebViewAssetLoader] serves that folder under
 * `https://appassets.androidplatform.net/assets/`.
 */
class MainActivity : ComponentActivity() {

    private companion object {
        const val APP_URL = "https://appassets.androidplatform.net/assets/app.html"
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val CHANNEL_ID = "matix_club"
        const val STATE_WEBVIEW = "matix_webview_state"
    }

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            filePathCallback = null
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val request = pendingPermissionRequest
            pendingPermissionRequest = null
            if (request != null) {
                if (result.values.all { it }) request.grant(request.resources) else request.deny()
            }
        }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        webView.addJavascriptInterface(MatixBridge(), "MatixNative")

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            loadWithOverviewMode = true
            useWideViewPort = true
            // The app has its own responsive layout; system font scaling would
            // break the labs-style grid, so pin the text zoom.
            textZoom = 100
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        applyWebViewDarkMode()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                val host = url.host ?: return false
                if (host == ASSET_HOST) return false
                if (request.isForMainFrame) {
                    // Open real links (docs, videos, etc.) in the browser.
                    return runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        true
                    }.getOrDefault(true)
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val needed = ArrayList<String>()
                if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    needed.add(Manifest.permission.CAMERA)
                }
                if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    needed.add(Manifest.permission.RECORD_AUDIO)
                }
                if (needed.isEmpty()) {
                    request.grant(request.resources)
                    return
                }
                val missing = needed.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) !=
                        PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    request.grant(request.resources)
                } else {
                    pendingPermissionRequest = request
                    permissionLauncher.launch(missing.toTypedArray())
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(arrayOf())
                filePathCallback = callback
                fileChooser.launch("*/*")
                return true
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }

        // Restore the page across process death instead of re-running the boot
        // screen and dropping the user back on sign-in.
        val saved = savedInstanceState?.getBundle(STATE_WEBVIEW)
        if (saved != null) {
            webView.restoreState(saved)
        } else {
            webView.loadUrl(APP_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        webView.saveState(state)
        outState.putBundle(STATE_WEBVIEW, state)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyWebViewDarkMode()
    }

    /**
     * The web app ships its own light and dark themes (toggled inside the app),
     * so the WebView must NOT auto-darken it. Force-darkening a page that is
     * already in light mode inverts text/background colours and is what made
     * some light-mode text unreadable on phones set to dark mode.
     */
    private fun applyWebViewDarkMode() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) return
        runCatching {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Club notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(arrayOf())
        filePathCallback = null
        super.onDestroy()
    }

    /** Exposed to the web app as `window.MatixNative`. */
    inner class MatixBridge {
        @JavascriptInterface
        fun notify(title: String, body: String) {
            runCatching {
                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title.ifBlank { "Matix the Math Club" })
                    .setContentText(body.ifBlank { "Open the app!" })
                    .setAutoCancel(true)
                NotificationManagerCompat.from(this@MainActivity)
                    .notify((System.currentTimeMillis() % 100_000L).toInt(), builder.build())
            }
        }
    }
}
