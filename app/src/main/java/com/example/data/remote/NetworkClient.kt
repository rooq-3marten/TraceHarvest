package com.example.data.remote

import com.example.data.remote.api.TraceHarvestApiService
import com.example.data.remote.model.AgentBatchSyncRequest
import com.example.data.remote.model.AgentBatchSyncResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manages HTTP networking, Retrofit instantiation and JWT authentication.
 */
object NetworkClient {

    const val DEFAULT_SERVER_URL = "https://admindashtrace-backend.vercel.app/"
    private var customBaseUrl: String? = null
    private var authToken: String? = null

    fun getServerUrl(): String = customBaseUrl ?: DEFAULT_SERVER_URL

    fun setServerUrl(url: String) {
        val trimmed = url.trim()
        val validUrl = if (trimmed.isNotBlank()) {
            if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        } else {
            DEFAULT_SERVER_URL
        }
        customBaseUrl = validUrl
        synchronized(this) {
            _apiService = null
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Low memory overhead on budget processors
        }
        OkHttpClient.Builder()
            .connectTimeout(35, TimeUnit.SECONDS) // Accommodates 2G/EDGE cellular handshake
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider = { authToken }))
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var _apiService: TraceHarvestApiService? = null

    val apiService: TraceHarvestApiService
        get() {
            return _apiService ?: synchronized(this) {
                _apiService ?: Retrofit.Builder()
                    .baseUrl(getServerUrl())
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(TraceHarvestApiService::class.java)
                    .also { _apiService = it }
            }
        }

    /**
     * Optional mock response hook for offline unit and JVM Robolectric testing.
     */
    @Volatile
    var mockResponseForTesting: AgentBatchSyncResponse? = null

    /**
     * Executes the batch sync against FastAPI. Returns null when the server is unreachable
     * or rejects the request, so records stay pending and retry later (no fake "synced").
     */
    suspend fun executeResilientBatchSync(request: AgentBatchSyncRequest): AgentBatchSyncResponse? {
        mockResponseForTesting?.let { return it }

        return try {
            val response = apiService.syncBatch(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val upstreamResponse = apiService.syncUpstream(request)
                if (upstreamResponse.isSuccessful && upstreamResponse.body() != null) {
                    upstreamResponse.body()!!
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
