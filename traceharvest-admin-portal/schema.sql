-- ==============================================================
-- TraceHarvest Central Export Registry - PostgreSQL + PostGIS Schema
-- ==============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Farmers Registry
CREATE TABLE IF NOT EXISTS farmers (
    id SERIAL PRIMARY KEY,
    client_uuid VARCHAR(64) UNIQUE NOT NULL,
    official_farmer_id VARCHAR(32) UNIQUE NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    state VARCHAR(50) NOT NULL,
    lga VARCHAR(50) NOT NULL,
    community VARCHAR(100),
    crop VARCHAR(50) NOT NULL,
    farm_size_hectares NUMERIC(8, 2) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geom GEOMETRY(Point, 4326),
    gps_polygon TEXT,
    cooperative_name VARCHAR(150),
    agent_id VARCHAR(50) NOT NULL,
    created_at_epoch_ms BIGINT NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_farmers_geom ON farmers USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_farmers_state_lga ON farmers(state, lga);
CREATE INDEX IF NOT EXISTS idx_farmers_crop ON farmers(crop);

-- 2. Good Agricultural Practices (GAP) & Chemical Applications
CREATE TABLE IF NOT EXISTS practice_logs (
    id SERIAL PRIMARY KEY,
    client_uuid VARCHAR(64) UNIQUE NOT NULL,
    farmer_client_uuid VARCHAR(64) NOT NULL REFERENCES farmers(client_uuid) ON DELETE CASCADE,
    farmer_code VARCHAR(32) NOT NULL,
    practice_type VARCHAR(100) NOT NULL,
    product_name VARCHAR(150),
    active_ingredient VARCHAR(150),
    dosage VARCHAR(100),
    quantity_used NUMERIC(10, 2),
    quantity_unit VARCHAR(30),
    date_applied_epoch_ms BIGINT NOT NULL,
    pre_harvest_interval_days INT DEFAULT 0,
    nafdac_reg_no VARCHAR(50),
    nafdac_approved BOOLEAN DEFAULT TRUE,
    gps_coordinates VARCHAR(100),
    risk_level VARCHAR(30) DEFAULT 'COMPLIANT',
    agent_id VARCHAR(50) NOT NULL,
    verification_photo_uri TEXT,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_practice_logs_farmer_code ON practice_logs(farmer_code);
CREATE INDEX IF NOT EXISTS idx_practice_logs_nafdac_reg ON practice_logs(nafdac_reg_no);

-- 3. Export Consignment Batches
CREATE TABLE IF NOT EXISTS export_batches (
    id SERIAL PRIMARY KEY,
    batch_number VARCHAR(64) UNIQUE NOT NULL,
    crop VARCHAR(50) NOT NULL,
    target_destination VARCHAR(100) DEFAULT 'European Union (Rotterdam)',
    total_tonnage NUMERIC(10, 2) NOT NULL,
    contributing_farmer_count INT NOT NULL,
    nafdac_export_clearance_status VARCHAR(50) DEFAULT 'PENDING_CLEARANCE',
    tamper_proof_sha256_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
