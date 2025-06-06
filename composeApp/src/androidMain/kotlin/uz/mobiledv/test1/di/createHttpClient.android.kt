package uz.mobiledv.test1.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createPlatformSpecificHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        expectSuccess = true // Important for Ktor to throw exceptions on non-2xx responses

        defaultRequest {
//            url("") // Base URL for all requests
//            contentType(ContentType.Application.Json)
//            headers.append("X-Appwrite-Project", "")
            // X-Appwrite-Key for admin tasks (server-side)
            // X-Appwrite-Session for client-side (after login) - this needs to be added dynamically
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Important for Appwrite models
            })
        }
        install(Logging) {
            level = LogLevel.ALL // Or LogLevel.BODY for production if needed
        }

        engine {
            config {
                connectTimeout(20, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                callTimeout(90, TimeUnit.SECONDS) // Overall timeout for the entire call
            }
        }
    }
}