package com.tfg.feature.news.data

import com.tfg.domain.model.NewsArticle
import com.tfg.domain.model.NewsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class NewsFeedRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val rssDateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    )

    private val feeds = mapOf(
        "https://www.coindesk.com/arc/outboundfeeds/rss/" to NewsSource.COINDESK,
        "https://cointelegraph.com/rss" to NewsSource.COINTELEGRAPH,
        "https://www.reddit.com/r/CryptoCurrency/hot.json?limit=25" to NewsSource.REDDIT_CRYPTO,
        "https://www.reddit.com/r/Bitcoin/hot.json?limit=25" to NewsSource.REDDIT_BITCOIN,
        "https://min-api.cryptocompare.com/data/v2/news/?lang=EN&sortOrder=popular" to NewsSource.CRYPTOCOMPARE
    )

    suspend fun fetchAllNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        feeds.map { (url, source) ->
            async {
                try {
                    when (source) {
                        NewsSource.REDDIT_CRYPTO, NewsSource.REDDIT_BITCOIN -> fetchReddit(url, source)
                        NewsSource.CRYPTOCOMPARE -> fetchCryptoCompare(url)
                        else -> fetchRss(url, source)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().sortedByDescending { it.publishedAt }
    }

    suspend fun fetchBySource(source: NewsSource): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = feeds.entries.firstOrNull { it.value == source }?.key ?: return@withContext emptyList()
        try {
            when (source) {
                NewsSource.REDDIT_CRYPTO, NewsSource.REDDIT_BITCOIN -> fetchReddit(url, source)
                NewsSource.CRYPTOCOMPARE -> fetchCryptoCompare(url)
                else -> fetchRss(url, source)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchRss(url: String, source: NewsSource): List<NewsArticle> {
        val request = Request.Builder().url(url)
            .header("User-Agent", "TFGApp/1.0")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return emptyList()
        return parseRss(body, source)
    }

    private fun parseRss(xml: String, source: NewsSource): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var title = ""
        var description = ""
        var link = ""
        var pubDate = ""
        var imageUrl: String? = null
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "item", "entry" -> {
                            inItem = true
                            title = ""; description = ""; link = ""; pubDate = ""; imageUrl = null
                        }
                        "media:content", "media:thumbnail", "enclosure" -> {
                            if (inItem) {
                                val mediaUrl = parser.getAttributeValue(null, "url")
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                if (mediaUrl != null && (type.startsWith("image") || mediaUrl.matches(Regex(".*\\.(jpg|jpeg|png|webp).*", RegexOption.IGNORE_CASE)))) {
                                    imageUrl = mediaUrl
                                }
                            }
                        }
                        "link" -> {
                            if (inItem) {
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) link = href
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title = text
                            "description", "summary", "content:encoded" -> {
                                if (description.isEmpty()) description = text
                            }
                            "link" -> if (link.isEmpty()) link = text
                            "pubDate", "published", "updated" -> if (pubDate.isEmpty()) pubDate = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" || parser.name == "entry") {
                        if (title.isNotBlank()) {
                            // Extract image from description HTML if not found
                            if (imageUrl == null) {
                                imageUrl = extractImageFromHtml(description)
                            }
                            articles.add(
                                NewsArticle(
                                    id = UUID.nameUUIDFromBytes("$source$link".toByteArray()).toString(),
                                    title = cleanHtml(title),
                                    description = cleanHtml(description).take(300),
                                    url = link,
                                    imageUrl = imageUrl,
                                    source = source,
                                    publishedAt = parseDate(pubDate)
                                )
                            )
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            parser.next()
        }
        return articles
    }

    private fun fetchReddit(url: String, source: NewsSource): List<NewsArticle> {
        val request = Request.Builder().url(url)
            .header("User-Agent", "TFGApp/1.0 (Android)")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return emptyList()
        return parseRedditJson(body, source)
    }

    private fun parseRedditJson(json: String, source: NewsSource): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val root = com.google.gson.JsonParser.parseString(json).asJsonObject
            val children = root.getAsJsonObject("data").getAsJsonArray("children")
            for (child in children) {
                val data = child.asJsonObject.getAsJsonObject("data")
                val title = data.get("title")?.asString ?: continue
                val selftext = data.get("selftext")?.asString ?: ""
                val permalink = data.get("permalink")?.asString ?: ""
                val created = data.get("created_utc")?.asDouble?.toLong() ?: 0L
                val thumbnail = data.get("thumbnail")?.asString
                val stickied = data.get("stickied")?.asBoolean ?: false
                val over18 = data.get("over_18")?.asBoolean ?: false

                if (stickied || over18) continue

                val imageUrl = when {
                    thumbnail != null && thumbnail.startsWith("http") -> thumbnail
                    else -> {
                        val preview = data.getAsJsonObject("preview")
                        preview?.getAsJsonArray("images")?.firstOrNull()
                            ?.asJsonObject?.getAsJsonObject("source")
                            ?.get("url")?.asString?.replace("&amp;", "&")
                    }
                }

                articles.add(
                    NewsArticle(
                        id = UUID.nameUUIDFromBytes("$source$permalink".toByteArray()).toString(),
                        title = title,
                        description = selftext.take(300),
                        url = "https://www.reddit.com$permalink",
                        imageUrl = imageUrl,
                        source = source,
                        publishedAt = created * 1000,
                        categories = data.get("link_flair_text")?.asString?.let { listOf(it) } ?: emptyList()
                    )
                )
            }
        } catch (_: Exception) { }
        return articles
    }

    private fun fetchCryptoCompare(url: String): List<NewsArticle> {
        val request = Request.Builder().url(url)
            .header("User-Agent", "TFGApp/1.0")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return emptyList()
        return parseCryptoCompareJson(body)
    }

    private fun parseCryptoCompareJson(json: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val root = com.google.gson.JsonParser.parseString(json).asJsonObject
            val data = root.getAsJsonArray("Data")
            for (item in data) {
                val obj = item.asJsonObject
                val title = obj.get("title")?.asString ?: continue
                val body = obj.get("body")?.asString ?: ""
                val url = obj.get("url")?.asString ?: ""
                val imageUrl = obj.get("imageurl")?.asString
                val publishedOn = obj.get("published_on")?.asLong ?: 0L
                val categories = obj.get("categories")?.asString?.split("|") ?: emptyList()

                articles.add(
                    NewsArticle(
                        id = UUID.nameUUIDFromBytes("${NewsSource.CRYPTOCOMPARE}$url".toByteArray()).toString(),
                        title = title,
                        description = body.take(300),
                        url = url,
                        imageUrl = imageUrl,
                        source = NewsSource.CRYPTOCOMPARE,
                        publishedAt = publishedOn * 1000,
                        categories = categories
                    )
                )
            }
        } catch (_: Exception) { }
        return articles
    }

    private fun parseDate(dateStr: String): Long {
        for (format in rssDateFormats) {
            try {
                return format.parse(dateStr)?.time ?: continue
            } catch (_: Exception) { continue }
        }
        return System.currentTimeMillis()
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractImageFromHtml(html: String): String? {
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""")
        return imgRegex.find(html)?.groupValues?.getOrNull(1)
    }
}
