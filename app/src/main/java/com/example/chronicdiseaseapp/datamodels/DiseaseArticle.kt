package com.example.chronicdiseaseapp.datamodels

data class DiseaseArticle(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val author: String = "",
    val publishedDate: Long = 0L,
    val tags: List<String> = emptyList()
)
