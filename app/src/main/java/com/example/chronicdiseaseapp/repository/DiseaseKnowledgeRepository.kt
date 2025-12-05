package com.example.chronicdiseaseapp.repository

import com.example.chronicdiseaseapp.datamodels.DiseaseArticle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DiseaseKnowledgeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val articlesCollection = firestore.collection("diseaseArticles")

    /**
     * Fetches disease articles from Firestore.
     *
     * IMPORTANT: This uses Source.SERVER to always fetch fresh data from the server,
     * preventing new users from seeing cached/stale data.
     *
     * @return Flow of Result containing list of disease articles
     */
    fun getDiseaseArticles(): Flow<Result<List<DiseaseArticle>>> = flow {
        try {
            // Use Source.SERVER to force fetch from server (no cache)
            // This ensures new users always get fresh data
            val snapshot = articlesCollection
                .orderBy("publishedDate", Query.Direction.DESCENDING)
                .get(Source.SERVER)  // Force server fetch, bypass cache
                .await()

            val articles = snapshot.documents.mapNotNull { doc ->
                try {
                    DiseaseArticle(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        author = doc.getString("author") ?: "",
                        publishedDate = doc.getLong("publishedDate") ?: 0L,
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList()
                    )
                } catch (e: Exception) {
                    null // Skip malformed documents
                }
            }

            emit(Result.success(articles))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Fetches a single article by ID.
     * Also uses Source.SERVER to ensure fresh data.
     */
    suspend fun getArticleById(articleId: String): Result<DiseaseArticle?> {
        return try {
            val doc = articlesCollection
                .document(articleId)
                .get(Source.SERVER)  // Force server fetch
                .await()

            if (doc.exists()) {
                val article = DiseaseArticle(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    content = doc.getString("content") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    category = doc.getString("category") ?: "",
                    author = doc.getString("author") ?: "",
                    publishedDate = doc.getLong("publishedDate") ?: 0L,
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }
                        ?: emptyList()
                )
                Result.success(article)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches articles by category.
     * Uses Source.SERVER for fresh data.
     */
    fun getArticlesByCategory(category: String): Flow<Result<List<DiseaseArticle>>> = flow {
        try {
            val snapshot = articlesCollection
                .whereEqualTo("category", category)
                .orderBy("publishedDate", Query.Direction.DESCENDING)
                .get(Source.SERVER)  // Force server fetch
                .await()

            val articles = snapshot.documents.mapNotNull { doc ->
                try {
                    DiseaseArticle(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        author = doc.getString("author") ?: "",
                        publishedDate = doc.getLong("publishedDate") ?: 0L,
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            emit(Result.success(articles))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
