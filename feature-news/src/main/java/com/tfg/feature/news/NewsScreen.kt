package com.tfg.feature.news

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tfg.core.ui.theme.*
import com.tfg.domain.model.NewsArticle
import com.tfg.domain.model.NewsSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val colors = TfgTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // ─── Header ───────────────────────────────────────────────
        NewsHeader(
            searchQuery = state.searchQuery,
            onSearchChange = viewModel::updateSearch,
            showBookmarksOnly = state.showBookmarksOnly,
            onToggleBookmarks = viewModel::toggleBookmarksOnly,
            bookmarkCount = state.bookmarkedIds.size
        )

        // ─── Source Filter Chips ──────────────────────────────────
        SourceFilterRow(
            selectedSource = state.selectedSource,
            onSourceSelected = viewModel::selectSource
        )

        // ─── Content ─────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentBlue, strokeWidth = 3.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("Loading crypto news...", color = colors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
                state.error != null && state.filteredArticles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, null, tint = colors.textTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.error ?: "", color = colors.textSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = viewModel::loadNews) {
                                Text("Retry", color = AccentBlue)
                            }
                        }
                    }
                }
                state.filteredArticles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = colors.textTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No articles found", color = colors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Featured hero card (first article with image)
                        val featured = state.filteredArticles.firstOrNull { it.imageUrl != null }
                        if (featured != null) {
                            item(key = "hero_${featured.id}") {
                                HeroNewsCard(
                                    article = featured,
                                    isBookmarked = state.bookmarkedIds.contains(featured.id),
                                    onBookmark = { viewModel.toggleBookmark(featured.id) },
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(featured.url)))
                                    }
                                )
                            }
                        }

                        // Trending section label
                        item(key = "trending_label") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Latest News",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${state.filteredArticles.size} articles",
                                    fontSize = 12.sp,
                                    color = colors.textTertiary
                                )
                            }
                        }

                        // Remaining articles
                        val remaining = if (featured != null) {
                            state.filteredArticles.filter { it.id != featured.id }
                        } else {
                            state.filteredArticles
                        }

                        items(remaining, key = { it.id }) { article ->
                            NewsArticleCard(
                                article = article,
                                isBookmarked = state.bookmarkedIds.contains(article.id),
                                onBookmark = { viewModel.toggleBookmark(article.id) },
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                                }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Header
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun NewsHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showBookmarksOnly: Boolean,
    onToggleBookmarks: () -> Unit,
    bookmarkCount: Int
) {
    val colors = TfgTheme.colors
    var showSearch by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Crypto News", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                Text(
                    "Decision helper • 5 sources",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Bookmark filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showBookmarksOnly) AccentGold.copy(alpha = 0.2f) else colors.surface)
                        .clickable { onToggleBookmarks() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (showBookmarksOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = if (showBookmarksOnly) AccentGold else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (bookmarkCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text("$bookmarkCount", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                // Search toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showSearch) AccentBlue.copy(alpha = 0.2f) else colors.surface)
                        .clickable { showSearch = !showSearch; if (!showSearch) onSearchChange("") }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (showSearch) AccentBlue else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Search bar
        AnimatedVisibility(visible = showSearch) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentBlue),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search news...", color = colors.textTertiary, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchChange("") }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Source Filter Chips
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SourceFilterRow(
    selectedSource: NewsSource?,
    onSourceSelected: (NewsSource?) -> Unit
) {
    val colors = TfgTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // "All" chip
        item {
            SourceChip(
                label = "All",
                color = AccentBlue,
                isSelected = selectedSource == null,
                onClick = { onSourceSelected(null) }
            )
        }
        items(NewsSource.entries.toList()) { source ->
            SourceChip(
                label = source.displayName,
                color = Color(source.color),
                isSelected = selectedSource == source,
                onClick = { onSourceSelected(if (selectedSource == source) null else source) }
            )
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tfgColors = TfgTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else tfgColors.surface)
            .border(
                1.dp,
                if (isSelected) color.copy(alpha = 0.5f) else tfgColors.border,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) color else tfgColors.textSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Hero / Featured Card
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun HeroNewsCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onClick: () -> Unit
) {
    val colors = TfgTheme.colors
    val sourceColor = Color(article.source.color)
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            if (article.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Source badge + Bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SourceBadge(source = article.source)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onBookmark() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AccentGold else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Bottom: Title + time
                Column {
                    Text(
                        article.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        timeAgo(article.publishedAt),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // FEATURED label
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-0.5).dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(sourceColor)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text("FEATURED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Regular Article Card
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onClick: () -> Unit
) {
    val colors = TfgTheme.colors
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            if (article.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder with source color
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(article.source.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = null,
                        tint = Color(article.source.color),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(4.dp))

                // Description preview
                if (article.description.isNotBlank()) {
                    Text(
                        article.description,
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Bottom: source + time + bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SourceBadge(source = article.source, compact = true)
                        Text(
                            "•",
                            fontSize = 10.sp,
                            color = colors.textTertiary
                        )
                        Text(
                            timeAgo(article.publishedAt),
                            fontSize = 11.sp,
                            color = colors.textTertiary
                        )
                    }
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) AccentGold else colors.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onBookmark() }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Source Badge
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SourceBadge(source: NewsSource, compact: Boolean = false) {
    val color = Color(source.color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            )
    ) {
        Text(
            source.displayName,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Time formatter
// ═══════════════════════════════════════════════════════════════════
private fun timeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> "${diff / 604_800_000}w ago"
    }
}
