package com.example.data.security

import java.security.MessageDigest
import java.util.UUID

class SecurityEngine(
    var config: SecurityEngineConfig = SecurityEngineConfig()
) {
    private var currentTokens = config.exporterRateLimitRpm
    private var lastRefillTime = System.currentTimeMillis()

    private val _eventLogs = mutableListOf<SecurityEvent>()
    val eventLogs: List<SecurityEvent> get() = _eventLogs.toList()

    init {
        seedInitialAuditEvents()
    }

    private fun seedInitialAuditEvents() {
        val now = System.currentTimeMillis()
        _eventLogs.addAll(
            listOf(
                SecurityEvent(
                    id = "SEC-EVT-9001",
                    timestamp = now - 180000,
                    clientIdentity = "Port of Rotterdam SPS Inspection Terminal",
                    clientIp = "145.22.89.14",
                    endpoint = "/api/v1/export/provenance/NG-SES-2026-0042",
                    method = "GET",
                    layer = SecurityLayer.MCP_SECURITY,
                    outcome = SecurityActionOutcome.ALLOWED_200,
                    httpStatus = 200,
                    reason = "Valid Bearer JWT token verified. Scopes [read:provenance, verify:sps] granted.",
                    payloadSnippet = "Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
                ),
                SecurityEvent(
                    id = "SEC-EVT-9002",
                    timestamp = now - 120000,
                    clientIdentity = "Anonymous Scanner / Scraper",
                    clientIp = "185.220.101.5",
                    endpoint = "/api/v1/export/provenance/batch?id=' UNION SELECT * FROM users--",
                    method = "GET",
                    layer = SecurityLayer.GUARDIAN_WAF,
                    outcome = SecurityActionOutcome.BLOCKED_403_FORBIDDEN_INJECTION,
                    httpStatus = 403,
                    reason = "SQL injection pattern matched: 'UNION SELECT'. In-memory event flagged.",
                    payloadSnippet = "Query param: id=' UNION SELECT * FROM users--"
                ),
                SecurityEvent(
                    id = "SEC-EVT-9003",
                    timestamp = now - 75000,
                    clientIdentity = "Automated High-Frequency Bot",
                    clientIp = "194.38.20.198",
                    endpoint = "/api/v1/batches/verify",
                    method = "GET",
                    layer = SecurityLayer.MCP_SECURITY,
                    outcome = SecurityActionOutcome.BLOCKED_429_RATE_LIMITED,
                    httpStatus = 429,
                    reason = "Exceeded sliding-window rate limit (60 req/min for unauthenticated clients).",
                    payloadSnippet = "Rate limit depleted: X-RateLimit-Remaining: 0"
                ),
                SecurityEvent(
                    id = "SEC-EVT-9004",
                    timestamp = now - 35000,
                    clientIdentity = "Compromised Terminal (Lagos)",
                    clientIp = "102.89.34.120",
                    endpoint = "/api/v1/inspections/mrl-signoff",
                    method = "POST",
                    layer = SecurityLayer.WALLARM_FIREWALL,
                    outcome = SecurityActionOutcome.BLOCKED_400_SCHEMA_VIOLATION,
                    httpStatus = 400,
                    reason = "Schema validation failed: Field 'moisture_percent' value -4.5 violates OpenAPI minimum: 0.0.",
                    payloadSnippet = "{\"batchCode\":\"NG-SES-2026-0042\",\"moisture_percent\":-4.5,\"mrl_override\":true}"
                ),
                SecurityEvent(
                    id = "SEC-EVT-9005",
                    timestamp = now - 15000,
                    clientIdentity = "Untrusted Client",
                    clientIp = "197.210.55.82",
                    endpoint = "/api/v1/export/certificates/generate",
                    method = "POST",
                    layer = SecurityLayer.MCP_SECURITY,
                    outcome = SecurityActionOutcome.BLOCKED_401_UNAUTHORIZED,
                    httpStatus = 401,
                    reason = "Missing Authorization Header. Exporter API requires JWT Bearer token.",
                    payloadSnippet = "Header: Authorization: None"
                )
            )
        )
    }

    fun generateJwt(role: ExporterRole, organization: String, subject: String): JwtToken {
        val now = System.currentTimeMillis()
        val expiresAt = now + 86400000L // 24 hours
        val header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
        val payload = """
            {"sub":"$subject","org":"$organization","role":"${role.name}","scopes":${role.allowedScopes.joinToString(",", "[", "]") { "\"$it\"" }},"iat":${now / 1000},"exp":${expiresAt / 1000}}
        """.trimIndent()

        val rawSignature = sha256Hex("$header.$payload.traceharvest_mcp_secret_salt_2026")
        val simulatedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + "." +
                rawSignature.take(24)

        return JwtToken(
            subject = subject,
            role = role,
            organization = organization,
            issuedAt = now,
            expiresAt = expiresAt,
            scopes = role.allowedScopes,
            rawToken = simulatedToken
        )
    }

    fun refillTokens() {
        val now = System.currentTimeMillis()
        val elapsedSec = (now - lastRefillTime) / 1000
        if (elapsedSec >= 60) {
            currentTokens = config.exporterRateLimitRpm
            lastRefillTime = now
        }
    }

    fun getRemainingTokens(): Int {
        refillTokens()
        return currentTokens
    }

    fun simulateRequest(
        endpoint: String,
        method: String,
        clientIp: String,
        clientIdentity: String,
        jwtToken: JwtToken?,
        payload: String = ""
    ): SecurityEvent {
        refillTokens()

        // 1. GuardianWAF Inspection (Pattern Matching for Injection / XSS)
        if (config.isGuardianWafActive) {
            val upperPayload = (endpoint + " " + payload).uppercase()
            val hasSqli = upperPayload.contains("UNION SELECT") ||
                    upperPayload.contains("UNION ALL SELECT") ||
                    upperPayload.contains("' OR 1=1") ||
                    upperPayload.contains("' OR '1'='1") ||
                    upperPayload.contains("DROP TABLE") ||
                    upperPayload.contains(";--") ||
                    upperPayload.contains("XP_CMDSHELL")

            if (hasSqli) {
                val event = SecurityEvent(
                    id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    timestamp = System.currentTimeMillis(),
                    clientIdentity = clientIdentity,
                    clientIp = clientIp,
                    endpoint = endpoint,
                    method = method,
                    layer = SecurityLayer.GUARDIAN_WAF,
                    outcome = SecurityActionOutcome.BLOCKED_403_FORBIDDEN_INJECTION,
                    httpStatus = 403,
                    reason = "GuardianWAF: Detected SQL injection pattern in query or payload.",
                    payloadSnippet = payload.ifBlank { endpoint }
                )
                _eventLogs.add(0, event)
                return event
            }

            val hasXss = upperPayload.contains("<SCRIPT") ||
                    upperPayload.contains("JAVASCRIPT:") ||
                    upperPayload.contains("ONERROR=") ||
                    upperPayload.contains("ONLOAD=") ||
                    upperPayload.contains("<SVG") ||
                    upperPayload.contains("<IFRAME") ||
                    upperPayload.contains("DOCUMENT.COOKIE") ||
                    Regex("ON[A-Z]+\\s*=", RegexOption.IGNORE_CASE).containsMatchIn(upperPayload)

            if (hasXss) {
                val event = SecurityEvent(
                    id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    timestamp = System.currentTimeMillis(),
                    clientIdentity = clientIdentity,
                    clientIp = clientIp,
                    endpoint = endpoint,
                    method = method,
                    layer = SecurityLayer.GUARDIAN_WAF,
                    outcome = SecurityActionOutcome.BLOCKED_403_FORBIDDEN_INJECTION,
                    httpStatus = 403,
                    reason = "GuardianWAF: Cross-Site Scripting (XSS) probe intercepted.",
                    payloadSnippet = payload.ifBlank { endpoint }
                )
                _eventLogs.add(0, event)
                return event
            }
        }

        // 2. Wallarm API Firewall (OpenAPI Schema Validation)
        if (config.isWallarmSchemaValidationActive && payload.isNotBlank()) {
            if (payload.contains("\"moisture_percent\":-") || payload.contains("\"mrl_override\":true") || payload.contains("\"tamper_hash\"")) {
                val event = SecurityEvent(
                    id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    timestamp = System.currentTimeMillis(),
                    clientIdentity = clientIdentity,
                    clientIp = clientIp,
                    endpoint = endpoint,
                    method = method,
                    layer = SecurityLayer.WALLARM_FIREWALL,
                    outcome = SecurityActionOutcome.BLOCKED_400_SCHEMA_VIOLATION,
                    httpStatus = 400,
                    reason = "Wallarm API Firewall: Request payload failed strict OpenAPI 3.1 specification.",
                    payloadSnippet = payload
                )
                _eventLogs.add(0, event)
                return event
            }
        }

        // 3. MCP Security: JWT Authentication
        if (config.isJwtEnforced && jwtToken == null) {
            val event = SecurityEvent(
                id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
                timestamp = System.currentTimeMillis(),
                clientIdentity = "Unauthenticated Client",
                clientIp = clientIp,
                endpoint = endpoint,
                method = method,
                layer = SecurityLayer.MCP_SECURITY,
                outcome = SecurityActionOutcome.BLOCKED_401_UNAUTHORIZED,
                httpStatus = 401,
                reason = "MCP Security: Missing or malformed JWT Bearer token.",
                payloadSnippet = "Header: Authorization: None"
            )
            _eventLogs.add(0, event)
            return event
        }

        // 4. MCP Security: Rate Limiting
        if (config.isRateLimitingEnabled) {
            if (currentTokens <= 0) {
                val event = SecurityEvent(
                    id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    timestamp = System.currentTimeMillis(),
                    clientIdentity = clientIdentity,
                    clientIp = clientIp,
                    endpoint = endpoint,
                    method = method,
                    layer = SecurityLayer.MCP_SECURITY,
                    outcome = SecurityActionOutcome.BLOCKED_429_RATE_LIMITED,
                    httpStatus = 429,
                    reason = "MCP Security Rate Limiter: Limit of ${config.exporterRateLimitRpm} req/min exceeded.",
                    payloadSnippet = "X-RateLimit-Limit: ${config.exporterRateLimitRpm}, Remaining: 0"
                )
                _eventLogs.add(0, event)
                return event
            }
            currentTokens--
        }

        // 5. Passed All Layers: 200 OK
        val event = SecurityEvent(
            id = "SEC-EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
            timestamp = System.currentTimeMillis(),
            clientIdentity = clientIdentity,
            clientIp = clientIp,
            endpoint = endpoint,
            method = method,
            layer = SecurityLayer.MCP_SECURITY,
            outcome = SecurityActionOutcome.ALLOWED_200,
            httpStatus = 200,
            reason = "Authorized query cleared all security filters. Scopes verified: ${jwtToken?.scopes?.joinToString() ?: "Public"}",
            payloadSnippet = "Status: 200 OK • Latency: 18ms"
        )
        _eventLogs.add(0, event)
        return event
    }

    fun resetRateLimitTokens() {
        currentTokens = config.exporterRateLimitRpm
        lastRefillTime = System.currentTimeMillis()
    }

    fun depleteRateLimitTokens() {
        currentTokens = 0
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
