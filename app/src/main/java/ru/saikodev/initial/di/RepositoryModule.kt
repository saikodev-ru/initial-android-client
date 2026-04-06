package ru.saikodev.initial.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.saikodev.initial.data.repository.AuthRepositoryImpl
import ru.saikodev.initial.data.repository.ChatRepositoryImpl
import ru.saikodev.initial.data.repository.ProfileRepositoryImpl
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.domain.repository.ChatRepository
import ru.saikodev.initial.domain.repository.ProfileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
