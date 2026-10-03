/**
 * TraceHarvest Admin Web Portal & Upstream Sync Engine
 * Built for high-reliability rural synchronization with Android devices.
 */

const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 8000;

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// In-Memory Database Store (with file persistence fallback)
const DATA_FILE = path.join(__dirname, 'data_store.json');

let db = {
  farmers: {},
  practices: [],
  batches: [],
  syncLogs: []
};

// Load saved data if exists
if (fs.existsSync(DATA_FILE)) {
  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf8');
    db = JSON.parse(raw);
    console.log(`[Storage] Loaded existing data: ${Object.keys(db.farmers).length} farmers, ${db.practices.length} practices.`);
  } catch (e) {
    console.error('[Storage] Error loading data file, starting with empty store:', e.message);
  }
}

function persistData() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (e) {
    console.error('[Storage] Failed to persist data:', e.message);
  }
}

// Serve Static Frontend Dashboard
app.use(express.static(path.join(__dirname, 'public')));

// ==============================================================
// 1. Upstream Sync Ingestion Endpoint (Called by Android App)
// ==============================================================
app.post('/api/v1/sync/upstream', (req, res) => {
  const { agent_id, device_timestamp_ms, farmers = [], practices = [] } = req.body;

  if (!agent_id) {
    return res.status(400).json({ error: 'agent_id is required' });
  }

  const assignedFarmerIds = {};
  const serverTimestamp = Date.now();

  // 1. Process Farmers (Idempotent by client_uuid)
  for (const farmer of farmers) {
    const clientUuid = farmer.client_uuid;
    if (!clientUuid) continue;

    if (db.farmers[clientUuid]) {
      // Already exists -> return existing assigned ID
      assignedFarmerIds[clientUuid] = db.farmers[clientUuid].official_farmer_id;
    } else {
      // Generate new official Farmer ID: TH-{STATE}-2026-{RANDOM}
      const stateStr = (farmer.state || 'NGR').trim();
      const stateCode = stateStr.length >= 3 ? stateStr.substring(0, 3).toUpperCase() : 'NGR';
      const randomCode = Math.floor(1000 + Math.random() * 9000);
      const officialId = `TH-${stateCode}-2026-${randomCode}`;

      db.farmers[clientUuid] = {
        ...farmer,
        official_farmer_id: officialId,
        server_received_at: serverTimestamp
      };
      assignedFarmerIds[clientUuid] = officialId;
    }
  }

  // 2. Process Practice Logs (Idempotent by client_uuid)
  for (const practice of practices) {
    const clientUuid = practice.client_uuid;
    if (!clientUuid) continue;

    const exists = db.practices.some(p => p.client_uuid === clientUuid);
    if (!exists) {
      // Evaluate Pre-Harvest Interval (PHI) clearance
      const appliedMs = practice.date_applied_epoch_ms || serverTimestamp;
      const phiDays = practice.pre_harvest_interval_days || 0;
      const safeHarvestMs = appliedMs + (phiDays * 86400000);
      const isPhiCleared = serverTimestamp >= safeHarvestMs;

      db.practices.push({
        ...practice,
        phi_cleared: isPhiCleared,
        safe_harvest_date_ms: safeHarvestMs,
        server_received_at: serverTimestamp
      });
    }
  }

  // 3. Log Sync Event
  db.syncLogs.unshift({
    agent_id,
    device_timestamp_ms,
    server_timestamp_ms: serverTimestamp,
    farmers_count: farmers.length,
    practices_count: practices.length
  });

  if (db.syncLogs.length > 50) db.syncLogs.pop();

  persistData();

  console.log(`[Sync] Agent ${agent_id} uploaded ${farmers.length} farmers and ${practices.length} practices.`);

  // Return standard AgentBatchSyncResponse
  res.json({
    status: 'synced',
    synced_farmers_count: farmers.length,
    synced_practices_count: practices.length,
    assigned_farmer_ids: assignedFarmerIds,
    server_timestamp_ms: serverTimestamp,
    message: `Batch processed with client_uuid idempotency (${farmers.length} farmers, ${practices.length} practices)`
  });
});

// ==============================================================
// 2. Admin Web Dashboard Endpoints
// ==============================================================

// Summary Statistics
app.get('/api/v1/admin/stats', (req, res) => {
  const farmersList = Object.values(db.farmers);
  const totalHectares = farmersList.reduce((acc, f) => acc + (parseFloat(f.farm_size_hectares) || 0), 0);
  const nonCompliantPractices = db.practices.filter(p => !p.nafdac_approved || p.risk_level === 'FLAGGED_HIGH_RISK').length;
  const activePhiHoldCount = db.practices.filter(p => !p.phi_cleared).length;

  res.json({
    total_farmers: farmersList.length,
    total_hectares: Math.round(totalHectares * 10) / 10,
    total_practices: db.practices.length,
    non_compliant_practices: nonCompliantPractices,
    active_phi_holds: activePhiHoldCount,
    total_batches: db.batches.length,
    last_sync_log: db.syncLogs[0] || null
  });
});

// Get Farmers List (with search & filter)
app.get('/api/v1/admin/farmers', (req, res) => {
  const { state, crop, search } = req.query;
  let result = Object.values(db.farmers);

  if (state && state !== 'ALL') {
    result = result.filter(f => f.state?.toLowerCase() === state.toLowerCase());
  }
  if (crop && crop !== 'ALL') {
    result = result.filter(f => f.crop?.toLowerCase() === crop.toLowerCase());
  }
  if (search) {
    const q = search.toLowerCase();
    result = result.filter(f =>
      f.full_name?.toLowerCase().includes(q) ||
      f.official_farmer_id?.toLowerCase().includes(q) ||
      f.phone_number?.includes(q)
    );
  }

  res.json(result);
});

// Get Practice Logs (with NAFDAC audit details)
app.get('/api/v1/admin/practices', (req, res) => {
  res.json(db.practices);
});

// Get or Create Export Consignment Batches
app.get('/api/v1/admin/batches', (req, res) => {
  res.json(db.batches);
});

app.post('/api/v1/admin/batches/create', (req, res) => {
  const { crop, destination, farmer_client_uuids = [], estimated_tonnage = 20.0 } = req.body;
  const batchNumber = `EXP-TH-2026-${Math.floor(1000 + Math.random() * 9000)}`;

  const newBatch = {
    id: db.batches.length + 1,
    batch_number: batchNumber,
    crop: crop || 'Sesame',
    destination: destination || 'Rotterdam, Netherlands (EU)',
    farmer_count: farmer_client_uuids.length,
    estimated_tonnage: parseFloat(estimated_tonnage) || 20.0,
    created_at_ms: Date.now(),
    export_clearance_status: 'CERTIFIED_COMPLIANT',
    tamper_proof_sha256: require('crypto').createHash('sha256').update(batchNumber + Date.now()).digest('hex')
  };

  db.batches.unshift(newBatch);
  persistData();
  res.json({ success: true, batch: newBatch });
});

// CSV Export for Regulatory Compliance
app.get('/api/v1/admin/export/csv', (req, res) => {
  const farmersList = Object.values(db.farmers);
  let csv = 'Official Farmer ID,Client UUID,Full Name,Phone,State,LGA,Community,Crop,Hectares,Latitude,Longitude,Agent ID\n';

  farmersList.forEach(f => {
    csv += `"${f.official_farmer_id}","${f.client_uuid}","${f.full_name}","${f.phone_number}","${f.state}","${f.lga}","${f.community || ''}","${f.crop}","${f.farm_size_hectares}","${f.latitude}","${f.longitude}","${f.agent_id}"\n`;
  });

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="traceharvest_farmers_registry.csv"');
  res.status(200).send(csv);
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`🌾 TraceHarvest Central Admin Portal & Ingestion API`);
  console.log(`🚀 Server running on: http://localhost:${PORT}`);
  console.log(`📡 Upstream Sync Endpoint: http://localhost:${PORT}/api/v1/sync/upstream`);
  console.log(`📱 Connect your Android App in Sync Tab -> Configure`);
  console.log(`=======================================================`);
});
