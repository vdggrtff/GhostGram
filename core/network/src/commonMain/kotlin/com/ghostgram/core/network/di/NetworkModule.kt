package com.ghostgram.core.network.di

import api.GeminiApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {

    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
        }
    }

    // Временно хардкодим ключ, потом переделаем на BYOK или сервер
        // single { GeminiApiClient(httpClient = get(), apiKey = "AQ.Ab8RN6K0K3GeqdC27Zj-71Jv6Uzk_iyKZqLEXgpAI06kGvBVOw") }

    single { GeminiApiClient(httpClient = get(), settingsManager = get()) }
}