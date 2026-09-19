package com.example.data.gateway

import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import java.util.Locale
import kotlin.random.Random

data class ProcessedInboundSms(
    val replyText: String,
    val automatedResponse: String,
    val actionTaken: String
)

class SmsEngine {

    fun processInboundMessage(
        fromPhone: String,
        messageText: String,
        farmers: List<FarmerEntity>,
        batches: List<HarvestBatchEntity>
    ): ProcessedInboundSms {
        val cleanText = messageText.trim()
        val upper = cleanText.uppercase(Locale.ROOT)
        val farmer = farmers.find { it.phoneNumber.replace(" ", "") == fromPhone.replace(" ", "") }
            ?: farmers.firstOrNull()

        return when {
            upper == "1" || upper.contains("CONFIRM") || upper == "EEY" || upper == "YES" -> {
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest Gateway: Na gode ${farmer?.fullName ?: "Farmer"}! Your harvest batch delivery has been digitally confirmed and signed on the export ledger.",
                    actionTaken = "BATCH_DIGITALLY_SIGNED"
                )
            }
            upper == "2" || upper.contains("DISPUTE") || upper == "NO" || upper == "AA" -> {
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest Gateway: Recorded. Field Extension Officer has been dispatched to re-inspect and resolve the batch record.",
                    actionTaken = "FLAGGED_FOR_INSPECTION"
                )
            }
            upper.contains("STATUS") -> {
                val latestBatch = batches.firstOrNull { it.farmerCode == farmer?.farmerCode } ?: batches.firstOrNull()
                val statusText = if (latestBatch != null) {
                    "Batch ${latestBatch.batchCode} Status: ${latestBatch.mrlStatus}. Moisture: ${latestBatch.moisturePercent}%. Destination: ${latestBatch.destinationMarket}."
                } else {
                    "No active batch record found for phone $fromPhone."
                }
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest Gateway: $statusText",
                    actionTaken = "STATUS_QUERY_RETURNED"
                )
            }
            upper.contains("SNIPER") || upper.contains("VIOLATION") -> {
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest URGENT: Chemical safety alert logged. The consignment will undergo NAFDAC residue laboratory testing prior to port transit.",
                    actionTaken = "RESIDUE_ALERT_ESCALATED"
                )
            }
            upper.contains("HELP") || upper.contains("TAIMAKO") -> {
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest SMS Help: Reply 'CONFIRM' to sign batch, 'STATUS' to check SPS clearance, or dial *384*748# free on MTN/Airtel/Glo.",
                    actionTaken = "HELP_RETURNED"
                )
            }
            else -> {
                ProcessedInboundSms(
                    replyText = cleanText,
                    automatedResponse = "TraceHarvest Gateway: Message received and logged to farmer code ${farmer?.farmerCode ?: "TH-GEN-001"}. Na gode!",
                    actionTaken = "GENERIC_LOGGED"
                )
            }
        }
    }
}
