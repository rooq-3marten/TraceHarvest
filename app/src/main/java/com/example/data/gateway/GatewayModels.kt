package com.example.data.gateway

enum class GatewayProviderType(
    val displayName: String,
    val description: String,
    val defaultEndpoint: String,
    val defaultUssdCode: String,
    val defaultSenderId: String,
    val telcoCoverage: String
) {
    AFRICAS_TALKING(
        displayName = "Africa's Talking",
        description = "Pan-African SMS & USSD APIs. Widely used with sandbox & high-throughput Nigerian routes.",
        defaultEndpoint = "https://api.africastalking.com/version1/messaging",
        defaultUssdCode = "*384*748#",
        defaultSenderId = "TraceHarvest",
        telcoCoverage = "MTN, Airtel, Glo, 9mobile"
    ),
    STREAMCOMM(
        displayName = "Streamcomm Limited (Lagos)",
        description = "Lagos-based messaging platform with direct operator SMPP connections and carrier billing.",
        defaultEndpoint = "https://api.streamcomm.ng/v2/sms/send",
        defaultUssdCode = "*384*748#",
        defaultSenderId = "TRACEHARVEST",
        telcoCoverage = "Direct Operator SMPP (Nigeria)"
    ),
    TELKOSH(
        displayName = "Telkosh Global Communications",
        description = "Direct operator connectivity, USSD gateway, SMPP, and bulk SMS (Lagos presence).",
        defaultEndpoint = "https://gateway.telkosh.com/api/v1/ussd",
        defaultUssdCode = "*7006*48#",
        defaultSenderId = "TRACE_NG",
        telcoCoverage = "Direct Telco Aggregator"
    ),
    REDTECH(
        displayName = "Redtech Ltd",
        description = "Africa-focused VAS & smart electronic communication channels for USSD and SMS.",
        defaultEndpoint = "https://api.redtech.ng/ussd/session",
        defaultUssdCode = "*384*748#",
        defaultSenderId = "TraceHarvest",
        telcoCoverage = "Value Added Services (VAS)"
    )
}

enum class UssdResponseType {
    CON, // Continue session
    END  // Terminate session
}

data class UssdRequest(
    val sessionId: String,
    val serviceCode: String,
    val phoneNumber: String,
    val text: String
)

data class UssdResponse(
    val responseType: UssdResponseType,
    val message: String
)

data class GatewayConfig(
    val provider: GatewayProviderType = GatewayProviderType.AFRICAS_TALKING,
    val apiKey: String = "atsk_live_f893a7c2901b4ef9_ng",
    val username: String = "traceharvest_prod",
    val shortCode: String = "34461",
    val ussdServiceCode: String = "*384*748#",
    val webhookCallbackUrl: String = "https://api.traceharvest.ng/v1/webhooks/ussd",
    val activeTelco: String = "MTN Nigeria"
)
