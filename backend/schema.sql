-- ==============================================================
-- TraceHarvest PostgreSQL Production Database Schema
-- Single Source of Truth for Android Agent App & Next.js Admin Dashboard
-- ==============================================================

-- 1. Users (Admin, Ops, Support, Analysts)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'admin' CHECK (role IN ('admin', 'ops', 'support', 'analyst')),
    full_name VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Field Agents
CREATE TABLE IF NOT EXISTS agents (
    id VARCHAR(64) PRIMARY KEY, -- e.g. AGENT-NG-042
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255),
    state VARCHAR(50) NOT NULL,
    cooperative VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'OFFLINE', 'SUSPENDED')),
    device_id VARCHAR(100),
    last_sync_at TIMESTAMP WITH TIME ZONE
);

-- 3. Farmers Registry
CREATE TABLE IF NOT EXISTS farmers (
    id VARCHAR(64) PRIMARY KEY, -- Client localId (UUID) or assigned ID
    farmer_code VARCHAR(32) UNIQUE NOT NULL, -- e.g. TH-KAN-2026-1048
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    crop_type VARCHAR(50) NOT NULL,
    cooperative VARCHAR(150),
    enrolled_by_agent_id VARCHAR(64) REFERENCES agents(id),
    source VARCHAR(30) NOT NULL DEFAULT 'agent', -- agent, ussd, web
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_farmers_code ON farmers(farmer_code);
CREATE INDEX IF NOT EXISTS idx_farmers_agent ON farmers(enrolled_by_agent_id);
CREATE INDEX IF NOT EXISTS idx_farmers_crop ON farmers(crop_type);

-- 4. Practice Logs (GAP & Chemical Audit)
CREATE TABLE IF NOT EXISTS practice_logs (
    id VARCHAR(64) PRIMARY KEY, -- Client localId (UUID)
    farmer_id VARCHAR(64) REFERENCES farmers(id) ON DELETE CASCADE,
    practice_type VARCHAR(100) NOT NULL,
    product_name VARCHAR(150),
    quantity DOUBLE PRECISION DEFAULT 1.0,
    log_date TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'agent' CHECK (source IN ('agent', 'ussd', 'web')),
    agent_id VARCHAR(64) REFERENCES agents(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_practice_logs_farmer ON practice_logs(farmer_id);
CREATE INDEX IF NOT EXISTS idx_practice_logs_agent ON practice_logs(agent_id);

-- 5. Consignment Batches
CREATE TABLE IF NOT EXISTS batches (
    id VARCHAR(64) PRIMARY KEY, -- Client localId (UUID)
    batch_code VARCHAR(64) UNIQUE NOT NULL, -- e.g. NG-SES-2026-0042-EXP
    agent_id VARCHAR(64) REFERENCES agents(id),
    total_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    quality_grade VARCHAR(50) NOT NULL DEFAULT 'Grade A Export Ready',
    aggregation_gps_lat DOUBLE PRECISION,
    aggregation_gps_lng DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batches_code ON batches(batch_code);

-- 6. Batch Farmers Join Table (Many-to-Many)
CREATE TABLE IF NOT EXISTS batch_farmers (
    batch_id VARCHAR(64) REFERENCES batches(id) ON DELETE CASCADE,
    farmer_id VARCHAR(64) REFERENCES farmers(id) ON DELETE CASCADE,
    PRIMARY KEY (batch_id, farmer_id)
);

-- 7. Exports Shipments
CREATE TABLE IF NOT EXISTS exports (
    id VARCHAR(64) PRIMARY KEY,
    shipment_code VARCHAR(64) UNIQUE NOT NULL,
    exporter_id VARCHAR(64) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_CLEARANCE' CHECK (status IN ('PENDING_CLEARANCE', 'CLEARED_EXPORT', 'IN_TRANSIT', 'DELIVERED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Export Batches Join Table (Many-to-Many)
CREATE TABLE IF NOT EXISTS export_batches (
    export_id VARCHAR(64) REFERENCES exports(id) ON DELETE CASCADE,
    batch_id VARCHAR(64) REFERENCES batches(id) ON DELETE CASCADE,
    PRIMARY KEY (export_id, batch_id)
);

-- 9. Sync Events (Audit Trail for Real-Time Sync Health per Agent)
CREATE TABLE IF NOT EXISTS sync_events (
    id SERIAL PRIMARY KEY,
    agent_id VARCHAR(64) REFERENCES agents(id),
    device_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL, -- PUSH, PULL, HEARTBEAT
    entity_type VARCHAR(50) NOT NULL, -- FARMER, PRACTICE_LOG, BATCH, BULK
    entity_id VARCHAR(64),
    status VARCHAR(30) NOT NULL CHECK (status IN ('success', 'failed', 'pending')),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sync_events_agent ON sync_events(agent_id);
CREATE INDEX IF NOT EXISTS idx_sync_events_created ON sync_events(created_at);
