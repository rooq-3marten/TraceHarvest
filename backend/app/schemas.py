from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

# --- Auth Schemas ---
class Token(BaseModel):
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int = 86400

class TokenData(BaseModel):
    email: Optional[str] = None
    role: Optional[str] = None

class LoginRequest(BaseModel):
    username: str # email or agent_id
    password: str

class UserCreate(BaseModel):
    email: str
    password: str
    role: str = "admin"
    full_name: Optional[str] = None

class UserOut(BaseModel):
    id: int
    email: str
    role: str
    full_name: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# --- Farmer Schemas ---
class FarmerCreate(BaseModel):
    id: Optional[str] = None # client localId
    client_uuid: Optional[str] = None
    farmer_code: Optional[str] = None
    name: Optional[str] = None
    full_name: Optional[str] = None
    phone: Optional[str] = None
    phone_number: Optional[str] = None
    gps_lat: Optional[float] = None
    latitude: Optional[float] = None
    gps_lng: Optional[float] = None
    longitude: Optional[float] = None
    crop_type: Optional[str] = None
    crop: Optional[str] = None
    cooperative: Optional[str] = None
    cooperative_name: Optional[str] = None
    state: Optional[str] = "Kano"
    lga: Optional[str] = "Dambatta"
    community: Optional[str] = ""
    agent_id: Optional[str] = "AGENT-NG-042"
    source: str = "agent"

class FarmerOut(BaseModel):
    id: str
    farmer_code: str
    name: str
    phone: str
    gps_lat: Optional[float] = None
    gps_lng: Optional[float] = None
    crop_type: str
    cooperative: Optional[str] = None
    enrolled_by_agent_id: Optional[str] = None
    source: str
    created_at: datetime
    synced_at: datetime

    class Config:
        from_attributes = True

# --- Practice Log Schemas ---
class PracticeLogCreate(BaseModel):
    id: Optional[str] = None # client localId
    client_uuid: Optional[str] = None
    farmer_id: Optional[str] = None
    farmer_client_uuid: Optional[str] = None
    farmer_code: Optional[str] = None
    practice_type: str
    product_name: Optional[str] = None
    quantity: float = 1.0
    quantity_used: Optional[float] = None
    dosage: Optional[str] = None
    log_date: Optional[datetime] = None
    date_applied_epoch_ms: Optional[int] = None
    source: str = "agent"
    agent_id: Optional[str] = "AGENT-NG-042"
    pre_harvest_interval_days: int = 0
    nafdac_reg_no: Optional[str] = None
    nafdac_approved: bool = True
    gps_coordinates: Optional[str] = None
    risk_level: str = "COMPLIANT"

class PracticeLogOut(BaseModel):
    id: str
    farmer_id: str
    practice_type: str
    product_name: Optional[str] = None
    quantity: float
    log_date: datetime
    source: str
    agent_id: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# --- Batch Schemas ---
class BatchCreate(BaseModel):
    id: Optional[str] = None
    client_uuid: Optional[str] = None
    batch_code: Optional[str] = None
    agent_id: Optional[str] = "AGENT-NG-042"
    crop: Optional[str] = "Sesame"
    total_quantity: float = 0.0
    quality_grade: str = "Grade A Export Ready"
    aggregation_gps_lat: Optional[float] = None
    aggregation_gps_lng: Optional[float] = None
    farmer_ids: List[str] = []
    farmer_codes: List[str] = []

class BatchOut(BaseModel):
    id: str
    batch_code: str
    agent_id: Optional[str] = None
    total_quantity: float
    quality_grade: str
    aggregation_gps_lat: Optional[float] = None
    aggregation_gps_lng: Optional[float] = None
    created_at: datetime

    class Config:
        from_attributes = True

# --- Bulk Sync Schemas ---
class PendingRecordItem(BaseModel):
    id: str
    entity_type: str # FARMER, PRACTICE_LOG, BATCH
    entity_id: str
    payload: Any # raw dict or string
    created_at: Optional[int] = None

class BulkSyncRequest(BaseModel):
    agent_id: str = "AGENT-NG-042"
    device_id: Optional[str] = None
    device_timestamp_ms: Optional[int] = None
    farmers: List[FarmerCreate] = []
    practices: List[PracticeLogCreate] = []
    batches: List[BatchCreate] = []
    records: List[PendingRecordItem] = []

class SyncRecordResult(BaseModel):
    id: str
    status: str # SYNCED, FAILED
    server_id: Optional[str] = None
    code: Optional[str] = None
    error: Optional[str] = None

class BulkSyncResponse(BaseModel):
    status: str = "success"
    synced_farmers_count: int = 0
    synced_practices_count: int = 0
    synced_batches_count: int = 0
    assigned_farmer_ids: Dict[str, str] = {}
    results: List[SyncRecordResult] = []
    server_timestamp_ms: int
    message: str

class SyncStatusResponse(BaseModel):
    agent_id: str
    last_sync_at: Optional[datetime] = None
    pending_count: int = 0
    status: str = "ACTIVE"
    sync_health: str = "HEALTHY" # HEALTHY, DEGRADED, OFFLINE

# --- Admin Dashboard Schemas (admindashtrace.vercel.app compatible) ---
class AdminOverviewKPI(BaseModel):
    total_farmers: int
    total_hectares: float
    total_batches: int
    total_tonnage: float
    active_agents: int
    export_ready_percentage: float
    pending_syncs_count: int
    action_required_count: int

class ActionRequiredItem(BaseModel):
    id: str
    title: str
    description: str
    severity: str # critical, warning, info
    entity_type: str
    entity_id: str
    timestamp: datetime

class ActivityItem(BaseModel):
    id: int
    timestamp: datetime
    agent_id: Optional[str] = None
    agent_name: Optional[str] = None
    event_type: str
    description: str
    status: str

class SystemStatusResponse(BaseModel):
    api_status: str = "OPERATIONAL"
    database_status: str = "CONNECTED"
    sync_latency_ms: int = 42
    active_agent_connections: int = 0
    sync_success_rate_percent: float = 99.8
    version: str = "1.0.0"
    server_time: datetime
