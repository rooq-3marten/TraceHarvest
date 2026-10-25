package com.example.data.remote

import com.example.core.auth.AuthTokenStore
import com.example.data.remote.api.TraceHarvestApiService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/** Endpoints that never carry a session token and are never retried after a 401. */
object AuthEndpoints {
    private val PATH_SUFFIXES = listOf("/auth/login", "/auth/agent/signup", "/auth/refresh")

    fun isAuthEndpoint(encodedPath: String): Boolean {
        val path = encodedPath.trimEnd('/')
        return PATH_SUFFIXES.any { path.endsWith(it) }
    }
}

/**
 * Silent session renewal.
 *
 * When the server answers 401 to an authenticated call this:
 *  1. takes a mutex, so any number of simultaneous 401s lead to ONE refresh call;
 *  2. if another call already renewed the token meanwhile, retries with the new token without refreshing again;
 *  3. otherwise POSTs auth/refresh with  Authorization: Bearer <stored refresh token>,
 *     saves the new access AND refresh tokens, and retries the original request once;
 *  4. if the server rejects the refresh token, expires the session (the app returns to Login).
 *     A network failure or server error during the refresh keeps the session so the agent can retry later.
 *
 * Local records are never touched by any of this.
 */
class TokenRefreshAuthenticator(
    private val store: AuthTokenStore,
    /** Resolved lazily: the API service is built from the very client that owns this authenticator. */
    private val api: () -> TraceHarvestApiService,
    private val onAccessTokenChanged: (String?) -> Unit = {}
) : Authenticator {

    private val mutex = Mutex()

    private sealed class RefreshOutcome {
        data class Renewed(val accessToken: String, val refreshToken: String?) : RefreshOutcome()
        object Rejected : RefreshOutcome()
        object Unavailable : RefreshOutcome()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (AuthEndpoints.isAuthEndpoint(request.url.encodedPath)) return null
        // Retry once only: if the retried request is also refused, give up.
        if (responseCount(response) >= 2) return null

        val sentToken = request.header("Authorization")
            ?.removePrefix("Bearer")
            ?.trim()
            .orEmpty()

        return runBlocking {
            mutex.withLock {
                val current = store.currentAccessToken()
                if (sentToken.isNotEmpty() && !current.isNullOrBlank() && current != sentToken) {
                    // A concurrent call already renewed the token while this one waited.
                    return@withLock request.withBearer(current)
                }

                val refreshToken = store.currentRefreshToken()
                if (refreshToken.isNullOrBlank()) return@withLock null

                when (val outcome = callRefresh(refreshToken)) {
                    is RefreshOutcome.Renewed -> {
                        store.saveRefreshedTokens(outcome.accessToken, outcome.refreshToken)
                        onAccessTokenChanged(outcome.accessToken)
                        request.withBearer(outcome.accessToken)
                    }
                    RefreshOutcome.Rejected -> {
                        store.expireSession()
                        onAccessTokenChanged(null)
                        null
                    }
                    RefreshOutcome.Unavailable -> null
                }
            }
        }
    }

    private suspend fun callRefresh(refreshToken: String): RefreshOutcome {
        return try {
            val response = api().refreshToken("Bearer $refreshToken")
            val body = response.body()
            when {
                response.isSuccessful && body != null && body.accessToken.isNotBlank() ->
                    RefreshOutcome.Renewed(body.accessToken, body.refreshToken)
                response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 422 ->
                    RefreshOutcome.Rejected
                else -> RefreshOutcome.Unavailable
            }
        } catch (e: IOException) {
            RefreshOutcome.Unavailable
        }
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
