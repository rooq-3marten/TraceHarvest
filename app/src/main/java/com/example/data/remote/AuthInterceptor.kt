package com.example.data.remote

import android.content.Context
import com.example.core.auth.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * OkHttp Interceptor responsible for:
 * 1. Attaching Authorization: Bearer <JWT> token to all outgoing network requests.
 * 2. Attaching idempotent X-Idempotency-Key UUID header on mutating HTTP calls.
 * 3. Intercepting HTTP 401 Unauthorized errors and clearing invalid sessions.
 */
class AuthInterceptor(
    private val context: Context? = null,
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // 1. Retrieve current JWT token
        val token = tokenProvider?.invoke()
            ?: context?.let { ctx ->
                try {
                    runBlocking { SessionManager.getInstance(ctx).getAuthToken() }
                } catch (e: Exception) {
                    null
                }
            }
            ?: NetworkClient.getAuthToken()

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        // 2. Attach Idempotency Key for safe network retries over 2G/EDGE
        if (original.method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            if (original.header("X-Idempotency-Key") == null) {
                builder.header("X-Idempotency-Key", UUID.randomUUID().toString())
            }
        }

        builder.header("Accept-Encoding", "gzip")

        val response = chain.proceed(builder.build())

        // 3. Catch 401 Unauthorized (Expired or Revoked Token)
        if (response.code == 401) {
            context?.let { ctx ->
                try {
                    runBlocking {
                        SessionManager.getInstance(ctx).clearSession()
                    }
                } catch (e: Exception) {
                    // Ignore on background thread
                }
            }
            NetworkClient.setAuthToken(null)
        }

        return response
    }
}
