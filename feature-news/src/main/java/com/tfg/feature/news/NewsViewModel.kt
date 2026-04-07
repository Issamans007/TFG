package com.tfg.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.NewsArticle
import com.tfg.domain.model.NewsSource
import com.tfg.feature.news.data.NewsFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val filteredArticles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedSource: NewsSource? = null,
    val searchQuery: String = "",
    val bookmarkedIds: Set<String> = emptySet(),
    val showBookmarksOnly: Boolean = false
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsFeedRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val articles = repository.fetchAllNews()
                _state.value = _state.value.copy(
                    articles = articles,
                    isLoading = false
                )
                applyFilters()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load news. Pull down to retry."
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            try {
                val articles = repository.fetchAllNews()
                _state.value = _state.value.copy(
                    articles = articles,
                    isRefreshing = false,
                    error = null
                )
                applyFilters()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }

    fun selectSource(source: NewsSource?) {
        _state.value = _state.value.copy(selectedSource = source)
        applyFilters()
    }

    fun updateSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun toggleBookmark(articleId: String) {
        val current = _state.value.bookmarkedIds.toMutableSet()
        if (current.contains(articleId)) current.remove(articleId) else current.add(articleId)
        _state.value = _state.value.copy(bookmarkedIds = current)
        applyFilters()
    }

    fun toggleBookmarksOnly() {
        _state.value = _state.value.copy(showBookmarksOnly = !_state.value.showBookmarksOnly)
        applyFilters()
    }

    private fun applyFilters() {
        val s = _state.value
        var result = s.articles

        // Source filter
        if (s.selectedSource != null) {
            result = result.filter { it.source == s.selectedSource }
        }

        // Search filter
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }

        // Bookmarks filter
        if (s.showBookmarksOnly) {
            result = result.filter { s.bookmarkedIds.contains(it.id) }
        }

        _state.value = _state.value.copy(filteredArticles = result)
    }
}
