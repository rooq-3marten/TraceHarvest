# TraceHarvest Technical & Synchronization Specification

This specification documents the exact data structures, cryptographic checks, and synchronization protocols implemented between the **TraceHarvest Android Field Agent App** and the **TraceHarvest Admin Web Portal**.

---

## 1. Upstream Sync Protocol

- **HTTP Method**: `POST`
- **Path**: `/api/v1/sync/upstream`
- **Request Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `User-Agent: TraceHarvest-Android/1.0`

### 1.1 Request Payload (`AgentBatchSyncRequest`)
```json
{
  "agent_id": "AGENT-NG-042",
  "device_timestamp_ms": 1727694000000,
  "farmers": [
    {
      "client_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "full_name": "Musa Ibrahim Dambatta",
      "phone_number": "+2348034512991",
      "state": "Kano",
      "lga": "Dambatta",
      "community": "Gwarabjawa",
      "crop": "Sesame",
      "farm_size_hectares": 4.5,
      "latitude": 12.4382,
      "longitude": 8.5147,
      "gps_polygon": "12.4374,8.5131;12.4374,8.5163;12.4342,8.5163;12.4342,8.5131",
      "cooperative_name": "Dambatta Sesame Growers Union",
      "agent_id": "AGENT-NG-042",
      "created_at_epoch_ms": 1727693000000
    }
  ],
  "practices": [
    {
      "client_uuid": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "farmer_client_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "farmer_code": "TH-KAN-2026-1048",
      "practice_type": "🧪 Pesticide application",
      "product_name": "Karate 5 EC",
      "active_ingredient": "Lambda-cyhalothrin (50 g/L EC)",
      "dosage": "400 ml",
      "quantity_used": 400.0,
      "quantity_unit": "ml",
      "date_applied_epoch_ms": 1727089200000,
      "pre_harvest_interval_days": 14,
      "nafdac_reg_no": "04-2015",
      "nafdac_approved": true,
      "gps_coordinates": "12.4382°N, 8.5147°E",
      "risk_level": "COMPLIANT",
      "agent_id": "AGENT-NG-042",
      "verification_photo_uri": null
    }
  ]
}
```

### 1.2 Response Payload (`AgentBatchSyncResponse`)
```json
{
  "status": "synced",
  "synced_farmers_count": 1,
  "synced_practices_count": 1,
  "assigned_farmer_ids": {
    "550e8400-e29b-41d4-a716-446655440000": "TH-KAN-2026-1048"
  },
  "server_timestamp_ms": 1727694005000,
  "message": "Batch processed with client_uuid idempotency (1 farmers, 1 practices)"
}
```

---

## 2. Idempotency & Conflict Resolution

Rural mobile environments experience frequent drops and intermittent 2G/EDGE networks.
- Every entity created on the mobile device is assigned a **`client_uuid` (v4 UUID)** before SQLite insertion.
- The server maintains a unique index on `client_uuid`:
  ```sql
  CREATE UNIQUE INDEX idx_farmers_client_uuid ON farmers(client_uuid);
  CREATE UNIQUE INDEX idx_practices_client_uuid ON practices(client_uuid);
  ```
- If the mobile app resends a batch because the previous HTTP response was interrupted:
  1. The server performs an `ON CONFLICT (client_uuid) DO UPDATE` or returns the previously assigned official ID.
  2. No duplicate farmer or practice rows are ever created.
  3. The client receives the assigned IDs and successfully marks records as `synced`.

---

## 3. Official ID Generation Rules

When a farmer is synced for the first time, the server generates an official registration ID with the following structure:
```
TH-{STATE_CODE}-2026-{RANDOM_4_DIGIT}
```
Examples:
- Kano State: `TH-KAN-2026-4821`
- Jigawa State: `TH-JIG-2026-9014`
- Kaduna State: `TH-KAD-2026-3392`
- Benue State: `TH-BEN-2026-7840`

---

## 4. NAFDAC Compliance & PHI Calculation

For export readiness to the EU, UK, and US markets, the server validates:
1. **Registered Chemical Check**: Does `nafdac_reg_no` match an active registered agrochemical?
2. **Pre-Harvest Interval (PHI)**:
   ```
   SafeHarvestDate = DateApplied + (PreHarvestIntervalDays * 86,400,000 ms)
   ```
   - If `CurrentDate < SafeHarvestDate`, the batch is flagged: **`RESIDUE_RISK: PHI Active`**.
   - If `CurrentDate >= SafeHarvestDate`, the batch is approved: **`COMPLIANT: PHI Cleared`**.
