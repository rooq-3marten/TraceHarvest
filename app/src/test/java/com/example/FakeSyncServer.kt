package com.example

import com.example.data.remote.model.AgentBatchSyncRequest
import com.example.data.remote.model.AgentBatchSyncResponse
import com.example.data.remote.model.SyncRecordResultDto

/**
 * A stand-in for the backend: it answers every record in a sync request.
 * By default every record is saved. Pass [rejected] (record id -> reason) to refuse some.
 * Pass [farmerCodes] (farmer client id -> server code) to hand out server farmer IDs.
 */
object FakeSyncServer {
    fun respond(
        request: AgentBatchSyncRequest,
        rejected: Map<String, String> = emptyMap(),
        farmerCodes: Map<String, String> = emptyMap()
    ): AgentBatchSyncResponse {
        val ids = request.farmers.map { it.clientUuid } +
            request.practices.map { it.clientUuid } +
            request.batches.map { it.clientUuid }
        val results = ids.map { id ->
            val reason = rejected[id]
            if (reason == null) {
                SyncRecordResultDto(id = id, status = "SYNCED", serverId = id, code = farmerCodes[id])
            } else {
                SyncRecordResultDto(id = id, status = "REJECTED_MISSING_FIELDS", error = reason)
            }
        }
        return AgentBatchSyncResponse(
            status = "success",
            assignedFarmerIds = farmerCodes,
            results = results
        )
    }

    /** Install this fake as the network answer. Call NetworkClient.mockResponderForTesting = null to remove. */
    fun install(
        rejected: Map<String, String> = emptyMap(),
        farmerCodes: Map<String, String> = emptyMap()
    ) {
        com.example.data.remote.NetworkClient.mockResponderForTesting = { request ->
            respond(request, rejected, farmerCodes)
        }
    }
}
