package com.tfg.domain.model

data class NewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val source: NewsSource,
    val publishedAt: Long,
    val categories: List<String> = emptyList()
)

enum class NewsSource(val displayName: String, val color: Long) {
    COINDESK("CoinDesk", 0xFF1A6DFF),
    COINTELEGRAPH("CoinTelegraph", 0xFF22B573),
    REDDIT_CRYPTO("Reddit r/Crypto", 0xFFFF4500),
    REDDIT_BITCOIN("Reddit r/Bitcoin", 0xFFF7931A),
    CRYPTOCOMPARE("CryptoCompare", 0xFF2196F3)
}
