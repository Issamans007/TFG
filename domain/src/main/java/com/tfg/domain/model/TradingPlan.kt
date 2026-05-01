package com.tfg.domain.model

/**
 * User-managed risk + execution plan that is **separate from strategy code**.
 *
 * The strategy (JS) decides *when* to buy/sell. The plan decides *how* to size,
 * exit, leverage, and which hours to allow. Engine merges plan onto every signal
 * before execution, so both live runner and backtester behave identically.
 *
 * Persisted as JSON inside [Script.params] under the key [PARAM_KEY].
 */
data class TradingPlan(
    val marketType: MarketType = MarketType.SPOT,
    val leverage: Int = 1,
    val marginType: MarginType = MarginType.ISOLATED,
    /** SELL signal opens SHORT (futures only). When false, SELL only closes longs. */
    val allowShort: Boolean = false,
    /** Notional % of equity per entry (overrides signal.sizePct unless signal sets explicitly). */
    val sizePct: Double = 2.0,
    val slMode: SlMode = SlMode.ATR,
    /** Meaning depends on [slMode]: ATR multiplier, percent, or absolute price offset. */
    val slValue: Double = 1.5,
    val tpMode: TpMode = TpMode.R_MULTIPLE,
    val tpLevels: List<TpLevel> = listOf(
        TpLevel(value = 1.5, qtyPct = 50.0),
        TpLevel(value = 3.0, qtyPct = 50.0)
    ),
    /** Move SL to entry once first TP hits. */
    val moveSlToBreakeven: Boolean = true,
    /** When true, restrict trading to [sessionStartHour, sessionEndHour) UTC. */
    val sessionEnabled: Boolean = false,
    val sessionStartHour: Int = 0,
    val sessionEndHour: Int = 24,
    /** Bars to wait after a closed trade before re-entering (0 = immediate). */
    val cooldownBars: Int = 0,
    val maxConcurrentTrades: Int = 1
) {
    enum class SlMode { ATR, PCT, FIXED_OFFSET }
    enum class TpMode { R_MULTIPLE, PCT }

    data class TpLevel(
        /** ATR/R-mult or percent depending on [TradingPlan.tpMode]. */
        val value: Double,
        /** Portion of position to close at this level (0-100). Sum of all levels should be 100. */
        val qtyPct: Double
    )

    fun isHourAllowed(epochMs: Long): Boolean {
        if (!sessionEnabled) return true
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = epochMs
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        return if (sessionStartHour <= sessionEndHour) {
            h in sessionStartHour until sessionEndHour
        } else {
            // Wrap (e.g. 22-04)
            h >= sessionStartHour || h < sessionEndHour
        }
    }

    fun toJson(): String {
        // Pipe-delimited key=value to avoid org.json dependency in :domain.
        // tps is encoded as "v:q,v:q,..."
        val tpStr = tpLevels.joinToString(",") { "${it.value}:${it.qtyPct}" }
        return listOf(
            "market=${marketType.name}",
            "lev=$leverage",
            "margin=${marginType.name}",
            "short=$allowShort",
            "size=$sizePct",
            "slMode=${slMode.name}",
            "sl=$slValue",
            "tpMode=${tpMode.name}",
            "tps=$tpStr",
            "be=$moveSlToBreakeven",
            "sess=$sessionEnabled",
            "h0=$sessionStartHour",
            "h1=$sessionEndHour",
            "cool=$cooldownBars",
            "maxC=$maxConcurrentTrades"
        ).joinToString("|")
    }

    companion object {
        const val PARAM_KEY = "__plan__"

        fun fromJson(s: String?): TradingPlan {
            if (s.isNullOrBlank()) return TradingPlan()
            return try {
                val map = mutableMapOf<String, String>()
                s.split("|").forEach { kv ->
                    val eq = kv.indexOf('=')
                    if (eq > 0) map[kv.substring(0, eq)] = kv.substring(eq + 1)
                }
                val tps = mutableListOf<TpLevel>()
                map["tps"]?.takeIf { it.isNotBlank() }?.split(",")?.forEach { row ->
                    val parts = row.split(":")
                    if (parts.size == 2) {
                        val v = parts[0].toDoubleOrNull() ?: return@forEach
                        val q = parts[1].toDoubleOrNull() ?: return@forEach
                        tps.add(TpLevel(v, q))
                    }
                }
                TradingPlan(
                    marketType = runCatching { MarketType.valueOf(map["market"] ?: "SPOT") }.getOrDefault(MarketType.SPOT),
                    leverage = (map["lev"]?.toIntOrNull() ?: 1).coerceIn(1, 125),
                    marginType = runCatching { MarginType.valueOf(map["margin"] ?: "ISOLATED") }.getOrDefault(MarginType.ISOLATED),
                    allowShort = map["short"]?.toBoolean() ?: false,
                    sizePct = map["size"]?.toDoubleOrNull() ?: 2.0,
                    slMode = runCatching { SlMode.valueOf(map["slMode"] ?: "ATR") }.getOrDefault(SlMode.ATR),
                    slValue = map["sl"]?.toDoubleOrNull() ?: 1.5,
                    tpMode = runCatching { TpMode.valueOf(map["tpMode"] ?: "R_MULTIPLE") }.getOrDefault(TpMode.R_MULTIPLE),
                    tpLevels = if (tps.isNotEmpty()) tps else listOf(TpLevel(1.5, 50.0), TpLevel(3.0, 50.0)),
                    moveSlToBreakeven = map["be"]?.toBoolean() ?: true,
                    sessionEnabled = map["sess"]?.toBoolean() ?: false,
                    sessionStartHour = (map["h0"]?.toIntOrNull() ?: 0).coerceIn(0, 23),
                    sessionEndHour = (map["h1"]?.toIntOrNull() ?: 24).coerceIn(0, 24),
                    cooldownBars = (map["cool"]?.toIntOrNull() ?: 0).coerceAtLeast(0),
                    maxConcurrentTrades = (map["maxC"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
                )
            } catch (_: Exception) { TradingPlan() }
        }

        fun fromScript(script: Script): TradingPlan = fromJson(script.params[PARAM_KEY])
    }
}

/** Returns this script's [TradingPlan] (defaults if none stored). */
fun Script.tradingPlan(): TradingPlan = TradingPlan.fromScript(this)

/** Returns a copy of this script with the given plan persisted in params. */
fun Script.withPlan(plan: TradingPlan): Script {
    val newParams = params.toMutableMap()
    newParams[TradingPlan.PARAM_KEY] = plan.toJson()
    return copy(params = newParams)
}
