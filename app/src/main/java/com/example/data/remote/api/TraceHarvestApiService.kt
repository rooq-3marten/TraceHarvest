package com.example.data.remote.api

import com.example.data.remote.model.AgentBatchSyncRequest
import com.example.data.remote.model.AgentBatchSyncResponse
import com.example.data.remote.model.AuthLoginRequest
import com.example.data.remote.model.AuthTokenResponse
import com.example.data.remote.model.BatchSyncDto
import com.example.data.remote.model.FarmerSyncDto
import com.example.data.remote.model.PracticeLogSyncDto
import com.example.data.remote.model.SyncStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit Interface communicating with FastAPI + PostgreSQL backend.
 * Endpoints are idempotent and designed for high-latency, rural connections.
 */
interface TraceHarvestApiService {

    /**
     * Primary bulk synchronization endpoint: accepts batch of pending records from Agent App.
     */
    @Headers("Content-Type: application/json")
    @POST("sync/batch")
    suspend fun syncBatch(
        @Body request: AgentBatchSyncRequest
    ): Response<AgentBatchSyncResponse>

    /**
     * Compatibility alias endpoint for upstream sync.
     */
    @Headers("Content-Type: application/json")
    @POST("api/v1/sync/upstream")
    suspend fun syncUpstream(
        @Body request: AgentBatchSyncRequest
    ): Response<AgentBatchSyncResponse>

    /**
     * Create single farmer record.
     */
    @Headers("Content-Type: application/json")
    @POST("farmers")
    suspend fun createFarmer(
        @Body farmer: FarmerSyncDto
    ): Response<Map<String, Any>>

    /**
     * Create single practice log.
     */
    @Headers("Content-Type: application/json")
    @POST("practice-logs")
    suspend fun createPracticeLog(
        @Body practiceLog: PracticeLogSyncDto
    ): Response<Map<String, Any>>

    /**
     * Create single consignment batch.
     */
    @Headers("Content-Type: application/json")
    @POST("batches")
    suspend fun createBatch(
        @Body batch: BatchSyncDto
    ): Response<Map<String, Any>>

    /**
     * Returns last sync timestamp and pending count for given agent.
     */
    @GET("sync/status/{agent_id}")
    suspend fun getSyncStatus(
        @Path("agent_id") agentId: String
    ): Response<SyncStatusResponse>

    /**
     * JWT Login for agents and operators.
     */
    @Headers("Content-Type: application/json")
    @POST("auth/login")
    suspend fun login(
        @Body credentials: AuthLoginRequest
    ): Response<AuthTokenResponse>

    /**
     * Refresh JWT token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") bearerToken: String
    ): Response<AuthTokenResponse>
}
