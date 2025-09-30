package com.informatique.educationComposeVersion.data.database

import com.informatique.educationComposeVersion.data.database.entity.Article
import kotlinx.coroutines.flow.Flow

interface DatabaseService {
    //Saving
    suspend fun upsert(article: Article)
    fun getSavedArticles(): Flow<List<Article>>
    suspend fun deleteArticle(article: Article)

    //Caching
    fun getAllArticles(): Flow<List<Article>>
    fun deleteAllAndInsertAll(articles: List<Article>)
}