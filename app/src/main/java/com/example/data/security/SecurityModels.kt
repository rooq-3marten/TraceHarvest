package com.example.data.security

enum class SecurityLayer(
    val title: String,
    val description: String,
    val deploymentType: String
) {
    MCP_SECURITY(
        title = "MCP Security Framework",
        description = "FastAPI integrated library for JWT auth, sliding-window rate limiting, and RBAC authorization.",
        deploymentType = "Framework Middleware (Python/FastAPI)"
    ),
    WALLARM_FIREWALL(
        title = "API Firewall (Wallarm)",
        description = "Open-source reverse proxy validating exporter requests & responses against OpenAPI schemas.",
        deploymentType = "Envoy / Sidecar Reverse Proxy"
    ),
    GUARDIAN_WAF(
        title = "GuardianWAF",
        description = "Go-based lightweight WAF with in-memory event storage, pattern matching, and signature filtering.",
        deploymentType = "In-Process / Edge Filter"
    )
}

enum class ExporterRole(
    val roleName: String,
    val defaultRateLimitRpm: Int,
    val allowedScopes: List<String>
) {
    EU_INSPECTOR("EU Border Phytosanitary Inspector", 500, listOf("read:provenance", "verify:sps", "audit:mrl", "download:cert")),
    CERTIFIED_EXPORTER("NEPC Certified Commodity Exporter", 300, listOf("read:batch", "query:consignments", "download:passport")),
    NAFDAC_LAB_OFFICER("NAFDAC Quality Assurance Officer", 400, listOf("write:lab_result", "quarantine:batch", "read:provenance")),
    PUBLIC_BUYER("Public QR Provenance Query", 60, listOf("read:public_summary"))
}

data class JwtToken(
    val subject: String,
    val role: ExporterRole,
    val organization: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val scopes: List<String>,
    val rawToken: String
)

data class RateLimiterState(
    val capacity: Int = 60,
    val remainingTokens: Int = 60,
    val windowSeconds: Int = 60,
    val lastRefillTimestamp: Long = System.currentTimeMillis()
)

enum class SecurityActionOutcome {
    ALLOWED_200,
    BLOCKED_401_UNAUTHORIZED,
    BLOCKED_403_FORBIDDEN_INJECTION,
    BLOCKED_429_RATE_LIMITED,
    BLOCKED_400_SCHEMA_VIOLATION
}

data class SecurityEvent(
    val id: String,
    val timestamp: Long,
    val clientIdentity: String,
    val clientIp: String,
    val endpoint: String,
    val method: String,
    val layer: SecurityLayer,
    val outcome: SecurityActionOutcome,
    val httpStatus: Int,
    val reason: String,
    val payloadSnippet: String
)

data class SecurityEngineConfig(
    val isJwtEnforced: Boolean = true,
    val isRateLimitingEnabled: Boolean = true,
    val isWallarmSchemaValidationActive: Boolean = true,
    val isGuardianWafActive: Boolean = true,
    val publicRateLimitRpm: Int = 60,
    val exporterRateLimitRpm: Int = 300,
    val activeRole: ExporterRole = ExporterRole.EU_INSPECTOR
)
