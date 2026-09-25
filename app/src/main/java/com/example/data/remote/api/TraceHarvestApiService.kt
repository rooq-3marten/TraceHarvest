package com.example.data.remote.api

import com.example.data.remote.model.AgentBatchSyncRequest
import com.example.data.remote.model.AgentBatchSyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit Interface communicating with FastAPI backend.
 * Endpoints are idempotent and designed for high-latency, rural connections.
 */
interface TraceHarvestApiService {

    /**
     * Upstream batch synchronization endpoint.
     * Takes all pending SQLite records from the Android device, commits them
     * to PostgreSQL with client_uuid conflict protection, and returns assigned official IDs.
     */
    @Headers("Content-Type: application/json")
    @POST("api/v1/sync/upstream")
    suspend fun syncBatch(
        @Body request: AgentBatchSyncRequest
    ): Response<AgentBatchSyncResponse>
}
