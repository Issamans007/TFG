package com.tfg.feature.chart

import android.annotation.SuppressLint
import android.view.ViewGroup
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
    strategyPlotJson: String? = null,
    dashboardJson: String? = null,
    chartType: String = "candle",
    symbol: String = "",
    modifier: Modifier = Modifier,
    onDrawing: (String, String) -> Unit = { _, _ -> },
    savedDrawingsJson: String? = null,
    onDrawingsSync: (String) -> Unit = { _ -> }
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }

    // Switch active symbol BEFORE pushing new candles so old drawings get
    // saved per-symbol and cleared cleanly to avoid stale-coordinate artefacts.
    LaunchedEffect(isReady, symbol) {
        if (!isReady || symbol.isEmpty()) return@LaunchedEffect
        webView?.post {
            val safe = symbol.replace("'", "")
            webView?.evaluateJavascript("setSymbol('$safe')", null)
        }
    }

    // Inject saved drawings from Room DB after symbol is set
    LaunchedEffect(isReady, symbol, savedDrawingsJson) {
        if (!isReady || symbol.isEmpty() || savedDrawingsJson == null) return@LaunchedEffect
        webView?.post {
            val safe = savedDrawingsJson
                .replace("\\", "\\\\")
                .replace("'", "\\'")
            webView?.evaluateJavascript("setDrawings('$safe')", null)
        }
    }

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

    // Push strategy plot data (overlays/panels plotted via plot() API in JS strategies)
    LaunchedEffect(isReady, strategyPlotJson) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            if (strategyPlotJson != null) {
                val escaped = strategyPlotJson.replace("\\", "\\\\").replace("'", "\\'")
                webView?.evaluateJavascript("setStrategyPlot('$escaped')", null)
            }
        }
    }

    // Push strategy dashboard overlay (score table on the chart)
    LaunchedEffect(isReady, dashboardJson) {
        if (!isReady) return@LaunchedEffect
        webView?.post {
            if (dashboardJson != null) {
                val escaped = dashboardJson.replace("\\", "\\\\").replace("'", "\\'")
                webView?.evaluateJavascript("setDashboard('$escaped')", null)
            } else {
                webView?.evaluateJavascript("hideDashboard()", null)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Allow only the bundled chart asset; deny file:// outside assets,
                // any content:// access, and universal/file-from-file XSS vectors.
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
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

                    @JavascriptInterface
                    fun onDrawingsSynced(json: String) {
                        onDrawingsSync(json)
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isReady = true
                        // Force re-layout so the chart renders at correct size
                        view?.requestLayout()
                    }

                    // Lock down navigation: only the bundled chart asset is allowed.
                    // Any other URL (e.g. injected via a malicious indicator URL or
                    // a future external link) is silently dropped.
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        return !target.startsWith("file:///android_asset/")
                    }
                }
                loadUrl("file:///android_asset/chart.html")
                webView = this
            }
        },
        update = { view ->
            view.requestLayout()
        }
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
    // Deduplicate markers at the exact same time + signalType so the chart
    // doesn't render multiple identical arrows on top of each other.
    val seen = HashSet<String>()
    signals
        .sortedBy { it.openTime }
        .forEach { s ->
            val key = "${s.openTime}|${s.signalType.name}"
            if (!seen.add(key)) return@forEach
            val obj = JSONObject()
            obj.put("t", s.openTime / 1000)
            obj.put("type", s.signalType.name)
            if (s.label.isNotEmpty()) obj.put("label", s.label)
            if (s.orderType != "MARKET") obj.put("orderType", s.orderType)
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
