package com.stackbleedctrl.pollen.di

import com.stackbleedctrl.pollen.llm.LlmBackend
import com.stackbleedctrl.pollen.llm.LocalLlmManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindLlmBackend(impl: LocalLlmManager): LlmBackend
}
