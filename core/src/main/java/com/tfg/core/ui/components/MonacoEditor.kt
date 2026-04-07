package com.tfg.core.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tfg.core.ui.theme.AccentBlue
import com.tfg.core.ui.theme.DarkBorder
import com.tfg.core.ui.theme.DarkCard
import com.tfg.core.ui.theme.ThemeState

/**
 * CodeMirror Editor wrapper — WebView-based code editor with:
 * - Full autocomplete for TFG strategy functions (sma, ema, rsi, macd, etc.)
 * - Inline error markers (bracket matching + JS syntax validation)
 * - Syntax highlighting with custom dark/light themes
 * - Bracket matching, auto-close brackets & code folding
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MonacoEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 250.dp
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var lastPushedCode by remember { mutableStateOf("") }
    val isDark = ThemeState.isDark

    // Push code from Kotlin → JS when it changes externally
    LaunchedEffect(isReady, code) {
        if (!isReady) return@LaunchedEffect
        if (code != lastPushedCode) {
            lastPushedCode = code
            val escaped = code
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            webView?.post {
                webView?.evaluateJavascript("setCode('$escaped')", null)
            }
        }
    }

    // Sync theme when it changes
    LaunchedEffect(isReady, isDark) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("setTheme(${isDark})", null)
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    isNestedScrollingEnabled = true
                    setBackgroundColor(if (isDark) 0xFF1C2333.toInt() else 0xFFF6F8FA.toInt())

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onCodeChanged(newCode: String) {
                            lastPushedCode = newCode
                            onCodeChange(newCode)
                        }

                        @JavascriptInterface
                        fun onEditorReady() {
                            isReady = true
                        }
                    }, "Android")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Force re-layout so the WebView renders at correct size
                            view?.requestLayout()
                        }
                    }

                    loadUrl("file:///android_asset/monaco_editor.html")
                    webView = this
                }
            },
            update = { view ->
                view.requestLayout()
            }
        )
    }
}
