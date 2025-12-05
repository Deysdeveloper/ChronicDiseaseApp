package com.example.chronicdiseaseapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chronicdiseaseapp.datamodels.DiseaseArticle
import com.example.chronicdiseaseapp.repository.DiseaseKnowledgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiseaseKnowledgeState(
    val articles: List<DiseaseArticle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null
)

class DiseaseKnowledgeViewModel : ViewModel() {
    private val repository = DiseaseKnowledgeRepository()

    private val _uiState = MutableStateFlow(DiseaseKnowledgeState())
    val uiState: StateFlow<DiseaseKnowledgeState> = _uiState.asStateFlow()

    init {
        // Load articles on initialization
        fetchDiseaseKnowledge()
    }

    /**
     * Fetches disease articles from the repository.
     * This will always fetch fresh data from the server (no cached data).
     */
    fun fetchDiseaseKnowledge(category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val flow = if (category != null) {
                    repository.getArticlesByCategory(category)
                } else {
                    repository.getDiseaseArticles()
                }

                flow.collect { result ->
                    result.fold(
                        onSuccess = { articles ->
                            _uiState.value = DiseaseKnowledgeState(
                                articles = articles,
                                isLoading = false,
                                error = null,
                                selectedCategory = category
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load articles"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Refreshes the disease knowledge data.
     * This is useful for pull-to-refresh functionality.
     */
    fun refresh() {
        fetchDiseaseKnowledge(_uiState.value.selectedCategory)
    }

    /**
     * Filters articles by category.
     */
    fun filterByCategory(category: String?) {
        fetchDiseaseKnowledge(category)
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
