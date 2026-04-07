package com.tfg.domain.model

/** Identifiers for dashboard cards the user can show/hide/reorder */
enum class DashboardCardType(val displayName: String) {
    PORTFOLIO_OVERVIEW("Portfolio Overview"),
    QUICK_STATS("Quick Stats"),
    RISK_METRICS("Risk Metrics"),
    RECENT_SIGNALS("Recent Signals"),
    OPEN_POSITIONS("Open Positions"),
    PNL_CHART("PnL Chart"),
    BOT_STATUS("Bot Status"),
    DONATION_PROGRESS("Donation Progress")
}

data class DashboardCardConfig(
    val type: DashboardCardType,
    val visible: Boolean = true,
    val order: Int = 0
)
