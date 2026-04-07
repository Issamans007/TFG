package com.tfg.feature.news.di

import com.tfg.feature.news.data.NewsFeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NewsModule {

    @Provides
    @Singleton
    fun provideNewsFeedRepository(): NewsFeedRepository = NewsFeedRepository()
}
