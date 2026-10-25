package com.example

import com.example.core.auth.AuthTokenStore
import com.example.data.remote.AuthInterceptor
import com.example.data.remote.TokenRefreshAuthenticator
import com.example.data.remote.api.TraceHarvestApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Pure JVM tests of silent token refresh (no Android classes involved). */
class AuthRefreshTest {

    private class FakeStore(
        @Volatile var access: String? = "old-access",
        @Volatile var refresh: String? = "refresh-1"
    ) : AuthTokenStore {
        @Volatile var expired = false
        override suspend fun currentAccessToken() = access
        override suspend fun currentRefreshToken() = refresh
        override suspend fun saveRefreshedTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
            if (refreshToken != null) refresh = refreshToken
        }
        override suspend fun expireSession() {
            expired = true
            access = null
            refresh = null
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var store: FakeStore
    private lateinit var client: OkHttpClient
    private lateinit var api: TraceHarvestApiService
    private val refreshCalls = AtomicInteger(0)
    private val farmerCalls = AtomicInteger(0)

    @Volatile private var refreshStatus = 200

    @Before
    fun setUp() {
        store = FakeStore()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.endsWith("/auth/refresh") -> {
                        refreshCalls.incrementAndGet()
                        if (refreshStatus == 200) {
                            MockResponse().setResponseCode(200).setBody(
                                """{"access_token":"new-access","refresh_token":"refresh-2","token_type":"bearer","expires_in":604800}"""
                            )
                        } else {
                            MockResponse().setResponseCode(refreshStatus).setBody("{}")
                        }
                    }
                    else -> {
                        farmerCalls.incrementAndGet()
                        if (request.getHeader("Authorization") == "Bearer new-access") {
                            MockResponse().setResponseCode(200).setBody("{}")
                        } else {
                            MockResponse().setResponseCode(401).setBody("{}")
                        }
                    }
                }
            }
        }
        server.start()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(OkHttpClient())
            .build()
        api = retrofit.create(TraceHarvestApiService::class.java)

        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { store.access })
            .authenticator(TokenRefreshAuthenticator(store, { api }))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun get(path: String) =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    @Test
    fun `a 401 triggers one refresh and the request is retried with the new token`() {
        val response = get("/farmers")
        assertEquals(200, response.code)
        assertEquals(1, refreshCalls.get())
        assertEquals("new-access", store.access)
        assertEquals("refresh-2", store.refresh)
        response.close()
    }

    @Test
    fun `eight simultaneous 401s lead to a single refresh call`() {
        val pool = Executors.newFixedThreadPool(8)
        val results = (1..8).map { pool.submit<Int> { get("/farmers").use { it.code } } }
        val codes = results.map { it.get(20, TimeUnit.SECONDS) }
        pool.shutdown()
        assertTrue(codes.all { it == 200 })
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `a rejected refresh token expires the session`() {
        refreshStatus = 401
        val response = get("/farmers")
        assertEquals(401, response.code)
        assertTrue(store.expired)
        assertNull(store.access)
        response.close()
    }

    @Test
    fun `a server error during refresh keeps the session`() {
        refreshStatus = 503
        val response = get("/farmers")
        assertEquals(401, response.code)
        assertTrue(!store.expired)
        assertNotNull(store.refresh)
        response.close()
    }

    @Test
    fun `the interceptor never overwrites an explicit Authorization header`() {
        client.newCall(
            Request.Builder().url(server.url("/farmers")).header("Authorization", "Bearer explicit").build()
        ).execute().close()
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("Bearer explicit", recorded?.getHeader("Authorization"))
    }

    @Test
    fun `the login endpoint never carries a session token and is never refreshed`() {
        client.newCall(Request.Builder().url(server.url("/auth/login")).build()).execute().close()
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNull(recorded?.getHeader("Authorization"))
        assertEquals(0, refreshCalls.get())
    }
}
