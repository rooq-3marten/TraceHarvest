package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.auth.SessionManager
import com.example.data.local.AppDatabase
import com.example.data.repository.TraceHarvestRepository
import com.example.data.remote.AuthInterceptor
import com.example.data.remote.TokenRefreshAuthenticator
import com.example.data.remote.api.TraceHarvestApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Signing out because the session expired must never delete what the agent already collected. */
@RunWith(RobolectricTestRunner::class)
class SessionExpiryKeepsRecordsTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `rejected token refresh expires session and keeps all local records`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val sessions = SessionManager(ctx, FakeTokenCipher())
        sessions.saveSession("expired-access", "rejected-refresh", "TEST-AGENT-X", "Test Agent", "+2348030000000", "a@example.com")
        val repo = TraceHarvestRepository(db.traceHarvestDao(), signedInAgentId = { sessions.currentAgentIdOrNull() })

        val farmer = repo.registerFarmer(
            fullName = "Kept Farmer", phoneNumber = "+2348030000009", state = "Kano", lga = "L", community = "C",
            crop = "Sesame", latitude = 1.0, longitude = 1.0, agentId = "TEST-AGENT-X"
        )
        repo.logPractice(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", agentId = "TEST-AGENT-X"
        )
        repo.createBatch(
            farmerCode = farmer.farmerCode, farmerName = farmer.fullName, crop = "Sesame", region = "Kano",
            bagCount = 2, netWeightKg = 100.0, moisturePercent = 7.4, foreignMatterPercent = 0.5, agentId = "TEST-AGENT-X"
        )

        val server = MockWebServer()
        // 1st response: protected endpoint returns 401; 2nd response: /auth/refresh returns 401 (refresh rejected)
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.start()

        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val refreshApi = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(OkHttpClient())
                .build()
                .create(TraceHarvestApiService::class.java)

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor { runBlocking { sessions.currentAccessToken() } })
                .authenticator(TokenRefreshAuthenticator(sessions, { refreshApi }))
                .build()

            client.newCall(Request.Builder().url(server.url("/farmers")).build()).execute().close()
        } finally {
            server.shutdown()
        }

        assertFalse(sessions.hasValidSession())
        assertTrue(sessions.consumeSessionExpiredNotice())
        assertEquals(1, db.traceHarvestDao().getPendingSyncFarmers().size)
        assertEquals(1, db.traceHarvestDao().getPendingSyncPracticeLogs().size)
        assertEquals(1, db.traceHarvestDao().getPendingSyncBatches().size)
    }
}
