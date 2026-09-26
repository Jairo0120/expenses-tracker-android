package com.jldev.expensestracker.android.di

import com.jldev.expensestracker.android.BuildConfig
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.remote.AccessTokenProvider
import com.jldev.expensestracker.android.data.remote.ApiJson
import com.jldev.expensestracker.android.data.remote.AuthInterceptor
import com.jldev.expensestracker.android.data.sync.SyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authRepository: AuthRepository): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(AccessTokenProvider(authRepository::accessToken)))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    redactHeader("Authorization")
                },
            )
            // The prod API runs on Lambda; cold starts and a first full sync can be slow.
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            // Retrofit resolves relative paths against the last "/", so the prod ".../prod" needs one.
            .baseUrl(BuildConfig.API_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi = retrofit.create(SyncApi::class.java)
}
