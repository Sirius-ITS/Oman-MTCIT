package com.informatique.mtcit.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.informatique.mtcit.data.database.dao.ArticleDao
import com.informatique.mtcit.data.database.dao.SavedArticleDao
import com.informatique.mtcit.data.database.entity.Article
import com.informatique.mtcit.data.database.entity.SavedArticleEntity

@Database(entities = [SavedArticleEntity::class, Article::class], version = 1, exportSchema = false)
abstract class ArticleDatabase : RoomDatabase() {

    abstract fun getSavedArticleDao(): SavedArticleDao
    abstract fun getArticleDao(): ArticleDao

}