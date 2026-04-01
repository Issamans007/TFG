package com.tfg.engine

import com.tfg.domain.model.*
import app.cash.quickjs.QuickJs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QuickJS-based evaluator for custom indicators.
 *
 * Users write JS code with: function indicator(candles) { ... }
 * The function returns an object with overlays, panels, and optional signals.
 * Unlike ScriptEngine (bot), this returns chart plot data — not trading signals.
 *
 * JS API available to user code:
 *   - Point functions: sma, ema, rsi, atr, bollinger, macd, obv, vwap, supertrend, etc.
 *   - Series functions: smaSeries, emaSeries, rsiSeries, atrSeries, bollingerSeries,
 *                       macdSeries, obvSeries, vwapSeries, stochasticSeries
 */
@Singleton
class IndicatorEngine @Inject constructor() : IndicatorExecutor {

    private val gson = Gson()
    private val listType = object : TypeToken<List<Double?>>() {}.type

    @Volatile private var cachedJs: QuickJs? = null
    private var loadedCode: String? = null

    private val watchdog = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "indicator-watchdog").also { it.isDaemon = true }
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
    }

    override fun evaluate(code: String, candles: List<Candle>): IndicatorOutput {
        if (candles.isEmpty()) return IndicatorOutput()
        val js = getOrCreateJs(code)
        val timeoutFuture = watchdog.schedule({ closeJs() }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return try {
            val candlesJson = buildCandlesJson(candles)
            js.evaluate("var _candles = $candlesJson;", "data")
            val resultStr = js.evaluate(
                "JSON.stringify(indicator(_candles));",
                "call"
            )?.toString() ?: return IndicatorOutput()
            parseOutput(resultStr, candles)
        } catch (e: Exception) {
            Timber.w(e, "IndicatorEngine execution failed")
            closeJs()
            IndicatorOutput()
        } finally {
            timeoutFuture.cancel(false)
        }
    }

    @Synchronized
    private fun getOrCreateJs(code: String): QuickJs {
        val existing = cachedJs
        if (existing != null && loadedCode == code) return existing

        existing?.close()
        val js = QuickJs.create()
        js.evaluate(ScriptEngine.JS_INDICATOR_LIBRARY, "indicators")
        js.evaluate(code, "indicator")
        cachedJs = js
        loadedCode = code
        return js
    }

    @Synchronized
    private fun closeJs() {
        cachedJs?.close()
        cachedJs = null
        loadedCode = null
    }

    private fun buildCandlesJson(candles: List<Candle>): String {
        val sb = StringBuilder("[")
        candles.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            sb.append("{\"openTime\":${c.openTime},\"open\":${c.open},\"high\":${c.high},")
            sb.append("\"low\":${c.low},\"close\":${c.close},\"volume\":${c.volume}}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseOutput(json: String, candles: List<Candle>): IndicatorOutput {
        val clean = json.trim().removeSurrounding("\"").replace("\\\"", "\"")
        if (clean.isEmpty() || clean == "null" || clean == "undefined") return IndicatorOutput()

        return try {
            val map = gson.fromJson<Map<String, Any>>(clean, object : TypeToken<Map<String, Any>>() {}.type)
                ?: return IndicatorOutput()

            val overlays = parseOverlays(map["overlays"])
            val panels = parsePanels(map["panels"])
            val signals = parseSignals(map["signals"])
            val fills = parseFills(map["fills"])
            val hlines = parseHLines(map["hlines"])
            val bgcolor = parseBgColor(map["bgcolor"])
            val labels = parseLabels(map["labels"])

            IndicatorOutput(
                overlays = overlays, panels = panels, signals = signals,
                fills = fills, hlines = hlines, bgcolor = bgcolor, labels = labels
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse indicator output")
            IndicatorOutput()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOverlays(raw: Any?): List<PlotSeries> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val name = m["name"]?.toString() ?: return@mapNotNull null
            val values = parseDoubleList(m["values"])
            PlotSeries(
                name = name,
                values = values,
                color = m["color"]?.toString() ?: "#58A6FF",
                lineWidth = (m["lineWidth"] as? Number)?.toInt() ?: 2,
                lineStyle = (m["lineStyle"] as? Number)?.toInt() ?: 0
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePanels(raw: Any?): List<PanelSeries> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val name = m["name"]?.toString() ?: return@mapNotNull null
            val values = parseDoubleList(m["values"])
            val refLines = (m["lines"] as? List<*>)?.mapNotNull { line ->
                val lm = line as? Map<String, Any> ?: return@mapNotNull null
                RefLine(
                    value = (lm["value"] as? Number)?.toDouble() ?: return@mapNotNull null,
                    color = lm["color"]?.toString() ?: "#8B949E"
                )
            } ?: emptyList()
            val extraLines = (m["extraLines"] as? List<*>)?.mapNotNull { el ->
                val em = el as? Map<String, Any> ?: return@mapNotNull null
                PlotSeries(
                    name = em["name"]?.toString() ?: return@mapNotNull null,
                    values = parseDoubleList(em["values"]),
                    color = em["color"]?.toString() ?: "#F0883E"
                )
            } ?: emptyList()
            PanelSeries(
                name = name,
                values = values,
                color = m["color"]?.toString() ?: "#9C27B0",
                type = m["type"]?.toString() ?: "line",
                refLines = refLines,
                extraLines = extraLines
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSignals(raw: Any?): List<IndicatorSignal> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val type = m["type"]?.toString() ?: return@mapNotNull null
            val index = (m["index"] as? Number)?.toInt() ?: return@mapNotNull null
            val price = (m["price"] as? Number)?.toDouble() ?: return@mapNotNull null
            IndicatorSignal(type = type, index = index, price = price)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFills(raw: Any?): List<FillBetween> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val line1 = m["line1"]?.toString() ?: return@mapNotNull null
            val line2 = m["line2"]?.toString() ?: return@mapNotNull null
            FillBetween(line1 = line1, line2 = line2, color = m["color"]?.toString() ?: "#58A6FF33")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseHLines(raw: Any?): List<HLine> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val price = (m["price"] as? Number)?.toDouble() ?: return@mapNotNull null
            HLine(
                price = price,
                color = m["color"]?.toString() ?: "#58A6FF",
                lineStyle = (m["lineStyle"] as? Number)?.toInt() ?: 2,
                title = m["title"]?.toString() ?: ""
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseBgColor(raw: Any?): List<BgColor> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val index = (m["index"] as? Number)?.toInt() ?: return@mapNotNull null
            val color = m["color"]?.toString() ?: return@mapNotNull null
            BgColor(index = index, color = color)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLabels(raw: Any?): List<ChartLabel> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<String, Any> ?: return@mapNotNull null
            val index = (m["index"] as? Number)?.toInt() ?: return@mapNotNull null
            val text = m["text"]?.toString() ?: return@mapNotNull null
            ChartLabel(
                index = index,
                text = text,
                position = m["position"]?.toString() ?: "above",
                color = m["color"]?.toString() ?: "#FFFFFF"
            )
        }
    }

    private fun parseDoubleList(raw: Any?): List<Double?> {
        return when (raw) {
            is List<*> -> raw.map { (it as? Number)?.toDouble() }
            else -> emptyList()
        }
    }
}
