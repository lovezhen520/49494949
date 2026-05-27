package com.webide.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.webide.app.domain.model.ProjectFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WebAppInterface(
    private val context: Context,
    private val onLog: (String) -> Unit
) {
    private var locationManager: LocationManager? = null
    private var locationCallback: ((Double, Double) -> Unit)? = null

    @JavascriptInterface
    fun log(message: String) {
        onLog("[JS] $message")
    }

    @JavascriptInterface
    fun vibrate(duration: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """{
            "model": "${Build.MODEL}",
            "brand": "${Build.BRAND}",
            "version": "${Build.VERSION.RELEASE}",
            "sdk": ${Build.VERSION.SDK_INT}
        }""".trimIndent()
    }

    @JavascriptInterface
    fun getLocation(callbackId: String) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationCallback?.let {
                        it(location.latitude, location.longitude)
                    }
                    locationManager?.removeUpdates(this)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

class CORSWebViewClient(
    private val files: List<ProjectFile>,
    private val onLog: (String) -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        onLog("[Request] $url")

        val fileName = url.substringAfterLast("/").substringBefore("?")
        val file = files.find { it.name == fileName }
        
        if (file != null) {
            val mimeType = when (file.type) {
                com.webide.app.domain.model.FileType.HTML -> "text/html"
                com.webide.app.domain.model.FileType.CSS -> "text/css"
                com.webide.app.domain.model.FileType.JS -> "application/javascript"
                com.webide.app.domain.model.FileType.JSON -> "application/json"
                com.webide.app.domain.model.FileType.OTHER -> "text/plain"
            }
            return WebResourceResponse(
                mimeType,
                "UTF-8",
                file.content.byteInputStream()
            )
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLog("[Page Loaded] $url")
    }
}

@Composable
fun WebViewPreview(
    files: List<ProjectFile>,
    modifier: Modifier = Modifier,
    onLog: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val logs = remember { mutableStateListOf<String>() }
    
    val indexFile = files.find { it.name == "index.html" }
    val htmlContent = indexFile?.content ?: """
        <!DOCTYPE html>
        <html>
        <head><title>Error</title></head>
        <body><h1>index.html not found</h1></body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                webViewClient = CORSWebViewClient(files) { log ->
                    logs.add(log)
                    onLog(log)
                }

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            val log = "[Console] ${it.sourceId()}:${it.lineNumber()} - ${it.message()}"
                            logs.add(log)
                            onLog(log)
                        }
                        return true
                    }
                }

                addJavascriptInterface(
                    WebAppInterface(ctx) { log ->
                        logs.add(log)
                        onLog(log)
                    },
                    "Android"
                )

                val jsBridge = """
                    <script>
                        window.AndroidAPI = {
                            log: function(msg) { Android.log(msg); },
                            vibrate: function(duration) { Android.vibrate(duration || 100); },
                            getDeviceInfo: function() { return JSON.parse(Android.getDeviceInfo()); },
                            getLocation: function(callback) {
                                window.__locationCallback = callback;
                                Android.getLocation('location');
                            },
                            showToast: function(msg) { Android.showToast(msg); }
                        };
                        
                        window.__onLocationReceived = function(lat, lng) {
                            if (window.__locationCallback) {
                                window.__locationCallback({ latitude: lat, longitude: lng });
                            }
                        };
                        
                        console.log('Android API loaded');
                    </script>
                """.trimIndent()

                val fullHtml = htmlContent.replace(
                    "</head>",
                    "$jsBridge\n</head>"
                )

                loadDataWithBaseURL(
                    "http://localhost/",
                    fullHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            val updatedIndexFile = files.find { it.name == "index.html" }
            val updatedHtmlContent = updatedIndexFile?.content ?: htmlContent
            
            val jsBridge = """
                <script>
                    window.AndroidAPI = {
                        log: function(msg) { Android.log(msg); },
                        vibrate: function(duration) { Android.vibrate(duration || 100); },
                        getDeviceInfo: function() { return JSON.parse(Android.getDeviceInfo()); },
                        getLocation: function(callback) {
                            window.__locationCallback = callback;
                            Android.getLocation('location');
                        },
                        showToast: function(msg) { Android.showToast(msg); }
                    };
                    
                    window.__onLocationReceived = function(lat, lng) {
                        if (window.__locationCallback) {
                            window.__locationCallback({ latitude: lat, longitude: lng });
                        }
                    };
                </script>
            """.trimIndent()

            val fullHtml = updatedHtmlContent.replace(
                "</head>",
                "$jsBridge\n</head>"
            )

            webView.loadDataWithBaseURL(
                "http://localhost/",
                fullHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}
