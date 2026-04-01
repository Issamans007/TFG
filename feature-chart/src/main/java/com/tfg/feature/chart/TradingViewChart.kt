package com.tfg.feature.chart

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tfg.domain.model.Candle
import com.tfg.domain.model.IndicatorOutput
import com.tfg.domain.model.SignalMarker
import com.tfg.domain.model.SignalType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Compose wrapper around a WebView running TradingView Lightweight Charts.
 * All indicator data is computed in Kotlin and pushed to JS via evaluateJavascript.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewChart(
    candles: List<Candle>,
    signals: List<SignalMarker>,
    overlayIndicators: List<OverlayData>,
    subIndicators: List<SubIndicatorData>,
    livePrice: Double?,
    showDrawingTools: Boolean,
    supertrendData: SupertrendData?,
    ichimokuData: IchimokuFullData?,
    cloudOverlays: List<CloudOverlayData>,
    customIndicatorOutputs: List<IndicatorOutput> = emptyList(),
    chartType: String = "candle",
    modifier: Modifier = Modifier,
    onDrawing: (String, String) -> Unit = { _, _ -> }
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }

    // Push data when ready or when data changes
    LaunchedEffect(isReady, candles) {
        if (!isReady || candles.isEmpty()) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("setCandles('${candlesToJson(candles)}')", null)
        }
    }

    // Chart type
    LaunchedEffect(isReady, chartType) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("setChartType('$chartType')", null)
        }
    }

    LaunchedEffect(isReady, signals) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("setMarkers('${markersToJson(signals)}')", null)
        }
    }

    LaunchedEffect(isReady, overlayIndicators) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            // Build keep-list of all active overlay names (including supertrend, ichimoku, cloud)
            val activeNames = overlayIndicators.map { it.name } +
                    (supertrendData?.let { listOf("supertrend_up", "supertrend_dn") } ?: emptyList()) +
                    (ichimokuData?.let { listOf("ichi_tenkan", "ichi_kijun", "ichi_senkouA", "ichi_senkouB", "ichi_chikou") } ?: emptyList()) +
                    cloudOverlays.flatMap { listOf(it.name + "_upper", it.name + "_lower") }
            val keepJson = JSONArray(activeNames).toString().replace("'", "\\'")
            webView?.evaluateJavascript("removeOverlaysExcept('$keepJson')", null)

            overlayIndicators.forEach { ov ->
                val json = lineDataToJson(ov.data)
                webView?.evaluateJavascript(
                    "addOverlay('${ov.name}','$json','${ov.color}',${ov.lineWidth},${ov.lineStyle})", null
                )
            }
        }
    }

    LaunchedEffect(isReady, cloudOverlays) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            // If no cloud overlays, remove any leftover BB cloud series
            if (cloudOverlays.isEmpty()) {
                webView?.evaluateJavascript("removeOverlay('BB_upper');removeOverlay('BB_lower')", null)
            }
            cloudOverlays.forEach { co ->
                val uj = lineDataToJson(co.upper)
                val lj = lineDataToJson(co.lower)
                webView?.evaluateJavascript("addCloudOverlay('${co.name}','$uj','$lj','${co.color}')", null)
            }
        }
    }

    LaunchedEffect(isReady, supertrendData) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            if (supertrendData != null) {
                val gj = lineDataToJson(supertrendData.greenLine)
                val rj = lineDataToJson(supertrendData.redLine)
                webView?.evaluateJavascript("addSupertrendOverlay('$gj','$rj')", null)
            } else {
                webView?.evaluateJavascript("removeOverlay('supertrend_up');removeOverlay('supertrend_dn')", null)
            }
        }
    }

    LaunchedEffect(isReady, ichimokuData) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            if (ichimokuData != null) {
                val tj = lineDataToJson(ichimokuData.tenkan)
                val kj = lineDataToJson(ichimokuData.kijun)
                val saj = lineDataToJson(ichimokuData.senkouA)
                val sbj = lineDataToJson(ichimokuData.senkouB)
                val cj = lineDataToJson(ichimokuData.chikou)
                webView?.evaluateJavascript("addIchimokuOverlay('$tj','$kj','$saj','$sbj','$cj')", null)
            } else {
                webView?.evaluateJavascript("removeIchimoku()", null)
            }
        }
    }

    LaunchedEffect(isReady, subIndicators) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            // Remove sub-indicators that are no longer active
            val activeNames = subIndicators.map { it.name }
            val keepJson = JSONArray(activeNames).toString().replace("'", "\\'")
            webView?.evaluateJavascript("removeSubIndicatorsExcept('$keepJson')", null)

            subIndicators.forEach { si ->
                val json = lineDataToJson(si.mainData)
                webView?.evaluateJavascript(
                    "addSubIndicator('${si.name}','$json','${si.color}','${si.type}',${si.scaleTop},${si.scaleBottom},null)", null
                )
                si.extraLines.forEach { (lineName, lineData, lineColor) ->
                    val lj = lineDataToJson(lineData)
                    webView?.evaluateJavascript("addSubLine('${si.name}','$lineName','$lj','$lineColor')", null)
                }
            }
        }
    }

    LaunchedEffect(isReady, livePrice) {
        if (!isReady || livePrice == null) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("setLivePrice($livePrice)", null)
        }
    }

    LaunchedEffect(isReady, showDrawingTools) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            webView?.evaluateJavascript("showDrawingToolbar($showDrawingTools)", null)
        }
    }

    // Push custom indicator data (overlays, panels, signals from IndicatorEngine)
    LaunchedEffect(isReady, customIndicatorOutputs) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            if (customIndicatorOutputs.isEmpty()) {
                webView?.evaluateJavascript("clearCustomIndicators()", null)
            } else {
                val json = customIndicatorOutputsToJson(customIndicatorOutputs)
                webView?.evaluateJavascript("setCustomIndicators('$json')", null)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                // ─── Smooth rendering ────────────────────────
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                isScrollbarFadingEnabled = true
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                // Allow chart to handle its own touch events
                setOnTouchListener { v, event ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                setBackgroundColor(0xFF0D1117.toInt())

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onDrawingAdded(type: String, data: String) {
                        onDrawing(type, data)
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isReady = true
                    }
                }
                loadUrl("file:///android_asset/chart.html")
                webView = this
            }
        },
        update = { /* updates handled by LaunchedEffects above */ }
    )
}

// ─── Data Models ────────────────────────────────────────────────────

data class OverlayData(
    val name: String,
    val data: List<TimeValue>,
    val color: String,
    val lineWidth: Int = 2,
    val lineStyle: Int = 0 // 0=solid,1=dotted,2=dashed
)

data class SubIndicatorData(
    val name: String,
    val mainData: List<TimeValue>,
    val color: String,
    val type: String = "line", // "line" or "histogram"
    val scaleTop: Float = 0.8f,
    val scaleBottom: Float = 0.0f,
    val extraLines: List<Triple<String, List<TimeValue>, String>> = emptyList()
)

data class TimeValue(val time: Long, val value: Double)

data class SupertrendData(
    val greenLine: List<TimeValue>,
    val redLine: List<TimeValue>
)

data class IchimokuFullData(
    val tenkan: List<TimeValue>,
    val kijun: List<TimeValue>,
    val senkouA: List<TimeValue>,
    val senkouB: List<TimeValue>,
    val chikou: List<TimeValue>
)

data class CloudOverlayData(
    val name: String,
    val upper: List<TimeValue>,
    val lower: List<TimeValue>,
    val color: String
)

// ─── JSON Serialization ─────────────────────────────────────────────
private fun candlesToJson(candles: List<Candle>): String {
    val arr = JSONArray()
    candles.forEach { c ->
        val obj = JSONObject()
        obj.put("t", c.openTime / 1000) // JS expects seconds
        obj.put("o", c.open)
        obj.put("h", c.high)
        obj.put("l", c.low)
        obj.put("c", c.close)
        obj.put("v", c.volume)
        arr.put(obj)
    }
    return arr.toString().replace("'", "\\'")
}

private fun markersToJson(signals: List<SignalMarker>): String {
    val arr = JSONArray()
    signals.forEach { s ->
        val obj = JSONObject()
        obj.put("t", s.openTime / 1000)
        obj.put("type", when (s.signalType) {
            SignalType.BUY -> "BUY"
            SignalType.SELL -> "SELL"
            SignalType.CLOSE -> "CLOSE"
        })
        arr.put(obj)
    }
    return arr.toString().replace("'", "\\'")
}

private fun lineDataToJson(data: List<TimeValue>): String {
    val arr = JSONArray()
    data.forEach { tv ->
        val obj = JSONObject()
        obj.put("time", tv.time / 1000)
        obj.put("value", tv.value)
        arr.put(obj)
    }
    return arr.toString().replace("'", "\\'")
}

/**
 * Merge multiple IndicatorOutputs into a single JSON for the WebView.
 * Structure: { overlays: [...], panels: [...], signals: [...] }
 */
private fun customIndicatorOutputsToJson(outputs: List<IndicatorOutput>): String {
    val root = JSONObject()
    val overlaysArr = JSONArray()
    val panelsArr = JSONArray()
    val signalsArr = JSONArray()

    outputs.forEach { output ->
        output.overlays.forEach { plot ->
            val obj = JSONObject()
            obj.put("name", plot.name)
            obj.put("color", plot.color)
            obj.put("lineWidth", plot.lineWidth)
            obj.put("lineStyle", plot.lineStyle)
            val valuesArr = JSONArray()
            plot.values.forEach { v -> if (v != null) valuesArr.put(v) else valuesArr.put(JSONObject.NULL) }
            obj.put("values", valuesArr)
            overlaysArr.put(obj)
        }
        output.panels.forEach { panel ->
            val obj = JSONObject()
            obj.put("name", panel.name)
            obj.put("color", panel.color)
            obj.put("type", panel.type)
            val valuesArr = JSONArray()
            panel.values.forEach { v -> if (v != null) valuesArr.put(v) else valuesArr.put(JSONObject.NULL) }
            obj.put("values", valuesArr)
            val linesArr = JSONArray()
            panel.refLines.forEach { rl ->
                val lo = JSONObject()
                lo.put("value", rl.value)
                lo.put("color", rl.color)
                linesArr.put(lo)
            }
            obj.put("lines", linesArr)
            val extraArr = JSONArray()
            panel.extraLines.forEach { el ->
                val eo = JSONObject()
                eo.put("name", el.name)
                eo.put("color", el.color)
                val ev = JSONArray()
                el.values.forEach { v -> if (v != null) ev.put(v) else ev.put(JSONObject.NULL) }
                eo.put("values", ev)
                extraArr.put(eo)
            }
            obj.put("extraLines", extraArr)
            panelsArr.put(obj)
        }
        output.signals.forEach { sig ->
            val obj = JSONObject()
            obj.put("type", sig.type)
            obj.put("index", sig.index)
            obj.put("price", sig.price)
            signalsArr.put(obj)
        }
    }

    val fillsArr = JSONArray()
    val hlinesArr = JSONArray()
    val bgcolorArr = JSONArray()
    val labelsArr = JSONArray()

    outputs.forEach { output ->
        output.fills.forEach { fill ->
            val obj = JSONObject()
            obj.put("line1", fill.line1)
            obj.put("line2", fill.line2)
            obj.put("color", fill.color)
            fillsArr.put(obj)
        }
        output.hlines.forEach { hl ->
            val obj = JSONObject()
            obj.put("price", hl.price)
            obj.put("color", hl.color)
            obj.put("lineStyle", hl.lineStyle)
            obj.put("title", hl.title)
            hlinesArr.put(obj)
        }
        output.bgcolor.forEach { bg ->
            val obj = JSONObject()
            obj.put("index", bg.index)
            obj.put("color", bg.color)
            bgcolorArr.put(obj)
        }
        output.labels.forEach { lbl ->
            val obj = JSONObject()
            obj.put("index", lbl.index)
            obj.put("text", lbl.text)
            obj.put("position", lbl.position)
            obj.put("color", lbl.color)
            labelsArr.put(obj)
        }
    }

    root.put("overlays", overlaysArr)
    root.put("panels", panelsArr)
    root.put("signals", signalsArr)
    root.put("fills", fillsArr)
    root.put("hlines", hlinesArr)
    root.put("bgcolor", bgcolorArr)
    root.put("labels", labelsArr)
    return root.toString().replace("'", "\\'")
}
