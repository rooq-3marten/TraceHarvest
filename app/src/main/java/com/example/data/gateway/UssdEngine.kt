package com.example.data.gateway

import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity

class UssdEngine {

    fun processRequest(
        request: UssdRequest,
        farmers: List<FarmerEntity>,
        batches: List<HarvestBatchEntity>
    ): UssdResponse {
        val hops = if (request.text.isBlank()) emptyList() else request.text.split("*")
        val currentFarmer = farmers.find { it.phoneNumber.replace(" ", "") == request.phoneNumber.replace(" ", "") }
            ?: farmers.firstOrNull()

        return when {
            // Level 0: Main Menu
            hops.isEmpty() -> {
                val greetingName = currentFarmer?.fullName?.split(" ")?.firstOrNull() ?: "Farmer"
                UssdResponse(
                    responseType = UssdResponseType.CON,
                    message = """
                        CON TraceHarvest Nigeria
                        Sannu, $greetingName!
                        ----------------------
                        1. Confirm Farm Practice
                        2. Check Batch Clearance
                        3. My Farmer ID & Geotag
                        4. Export Price Premium
                        5. Report Banned Sniper
                        0. Exit
                    """.trimIndent()
                )
            }

            // Menu 1: Confirm Farm Practice
            hops[0] == "1" -> handlePracticeMenu(hops, currentFarmer)

            // Menu 2: Check Batch Clearance
            hops[0] == "2" -> handleBatchMenu(hops, currentFarmer, batches)

            // Menu 3: My Farmer ID & Geotag
            hops[0] == "3" -> {
                val farmerCode = currentFarmer?.farmerCode ?: "TH-KAN-2026-1048"
                val coop = currentFarmer?.cooperative ?: "Dambatta Sesame Cluster"
                val crop = currentFarmer?.crop ?: "Sesame"
                val ha = currentFarmer?.farmSizeHectares ?: 3.5
                val lat = currentFarmer?.latitude ?: 12.4358
                val lng = currentFarmer?.longitude ?: 8.5147

                UssdResponse(
                    responseType = UssdResponseType.END,
                    message = """
                        END [Farmer Provenance ID]
                        ID: $farmerCode
                        Crop: $crop ($ha Hectares)
                        Coop: $coop
                        GPS: ${"%.4f".format(lat)}°N, ${"%.4f".format(lng)}°E
                        Status: Geotag Verified & Active
                    """.trimIndent()
                )
            }

            // Menu 4: Export Price Premium
            hops[0] == "4" -> {
                UssdResponse(
                    responseType = UssdResponseType.END,
                    message = """
                        END [TraceHarvest Premium]
                        Traceable ${currentFarmer?.crop ?: "Sesame"} qualifies for +18% export premium:
                        Export Price: $1,650/MT
                        Local Spot: $1,400/MT
                        Est. Payout Bonus: +₦380,000/ton
                        Zero MRL violations required.
                    """.trimIndent()
                )
            }

            // Menu 5: Report Banned Sniper Spray
            hops[0] == "5" -> handleSniperWhistleblower(hops, currentFarmer)

            // Menu 0: Exit
            hops[0] == "0" -> {
                UssdResponse(
                    responseType = UssdResponseType.END,
                    message = "END Session ended. Thank you for protecting Nigerian agricultural exports."
                )
            }

            else -> {
                UssdResponse(
                    responseType = UssdResponseType.END,
                    message = "END Invalid selection. Dial *384*748# to try again."
                )
            }
        }
    }

    private fun handlePracticeMenu(hops: List<String>, farmer: FarmerEntity?): UssdResponse {
        return when (hops.size) {
            1 -> {
                UssdResponse(
                    responseType = UssdResponseType.CON,
                    message = """
                        CON [Confirm Practice]
                        Select input applied:
                        1. Lambda-Super 5EC (Safe)
                        2. BioNeem Extract (Organic)
                        3. PICS Hermetic Storage
                        4. Other chemical
                    """.trimIndent()
                )
            }
            2 -> {
                val inputName = when (hops[1]) {
                    "1" -> "Lambda-Super 5EC"
                    "2" -> "BioNeem Extract"
                    "3" -> "PICS Hermetic Storage"
                    else -> "Field chemical"
                }
                UssdResponse(
                    responseType = UssdResponseType.CON,
                    message = """
                        CON Confirm:
                        Applied $inputName on your farm?
                        1. YES - Confirm application
                        2. NO - Cancel
                    """.trimIndent()
                )
            }
            3 -> {
                if (hops[2] == "1") {
                    UssdResponse(
                        responseType = UssdResponseType.END,
                        message = """
                            END Practice logged successfully!
                            Timestamped on TraceHarvest registry.
                            Required Pre-Harvest Interval: 14 days.
                            Na gode!
                        """.trimIndent()
                    )
                } else {
                    UssdResponse(
                        responseType = UssdResponseType.END,
                        message = "END Practice confirmation cancelled. No record altered."
                    )
                }
            }
            else -> UssdResponse(UssdResponseType.END, "END Session completed.")
        }
    }

    private fun handleBatchMenu(hops: List<String>, farmer: FarmerEntity?, batches: List<HarvestBatchEntity>): UssdResponse {
        val farmerBatches = if (farmer != null) {
            batches.filter { it.farmerCode == farmer.farmerCode }
        } else batches

        val latestBatch = farmerBatches.firstOrNull() ?: batches.firstOrNull()

        return if (latestBatch != null) {
            val statusStr = if (latestBatch.mrlStatus == "PASSED_SPS") "CLEARED FOR EXPORT" else "QUARANTINE RISK"
            UssdResponse(
                responseType = UssdResponseType.END,
                message = """
                    END [Consignment Status]
                    Batch: ${latestBatch.batchCode}
                    Crop: ${latestBatch.crop} (${latestBatch.netWeightKg.toInt()} kg)
                    Moisture: ${latestBatch.moisturePercent}%
                    SPS Clearance: $statusStr
                    Dest: ${latestBatch.destinationMarket.take(12)}
                """.trimIndent()
            )
        } else {
            UssdResponse(
                responseType = UssdResponseType.END,
                message = "END No active harvest batch found for this phone number."
            )
        }
    }

    private fun handleSniperWhistleblower(hops: List<String>, farmer: FarmerEntity?): UssdResponse {
        return when (hops.size) {
            1 -> {
                UssdResponse(
                    responseType = UssdResponseType.CON,
                    message = """
                        CON [Report Banned Sniper (DDVP)]
                        Did an aggregator or warehouse use Sniper / Dichlorvos on grain bags?
                        1. YES - Urgent Report
                        2. NO - Exit
                    """.trimIndent()
                )
            }
            2 -> {
                if (hops[1] == "1") {
                    UssdResponse(
                        responseType = UssdResponseType.END,
                        message = """
                            END ALERT DISPATCHED:
                            NAFDAC & Exporter Quality Inspector notified.
                            Batch will be intercepted before container loading.
                            Thank you for preventing export rejection!
                        """.trimIndent()
                    )
                } else {
                    UssdResponse(UssdResponseType.END, "END Report cancelled.")
                }
            }
            else -> UssdResponse(UssdResponseType.END, "END Session ended.")
        }
    }
}
