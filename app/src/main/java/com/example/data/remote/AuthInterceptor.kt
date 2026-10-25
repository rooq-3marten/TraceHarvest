package com.example.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * OkHttp Interceptor responsible for:
 * 1. Attaching Authorization: Bearer <JWT> to outgoing requests, with two exceptions:
 *    - a request that already carries an Authorization header keeps it (the token-refresh call depends on this);
 *    - auth/login, auth/agent/signup and auth/refresh never get a session token.
 * 2. Attaching an idempotent X-Idempotency-Key UUID header on mutating HTTP calls.
 *
 * A 401 does NOT clear the session here. [TokenRefreshAuthenticator] renews the token silently and
 * only ends the session if the server rejects the refresh token.
 */
class AuthInterceptor(
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val skipToken = AuthEndpoints.isAuthEndpoint(original.url.encodedPath)
        if (!skipToken && original.header("Authorization") == null) {
            val token = tokenProvider?.invoke() ?: NetworkClient.getAuthToken()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }

        // Safe network retries over 2G/EDGE
        if (original.method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            if (original.header("X-Idempotency-Key") == null) {
                builder.header("X-Idempotency-Key", UUID.randomUUID().toString())
            }
        }

        // Accept-Encoding is intentionally not set by hand: OkHttp adds "gzip" itself and then
        // decompresses the response. Setting it manually turns that decompression off.
        return chain.proceed(builder.build())
    }
}
