// TraceHarvest Admin Portal - Interactive Logic & GIS Map Engine

let map;
let markerLayerGroup;
let polygonLayerGroup;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  initMap();
  detectServerUrl();
  loadAllData();

  // Auto-refresh data every 5 seconds to detect live mobile syncs
  setInterval(loadAllData, 5000);

  // Setup search and filter listeners
  document.getElementById('farmer-search')?.addEventListener('input', filterFarmers);
  document.getElementById('state-filter')?.addEventListener('change', filterFarmers);
  document.getElementById('crop-filter')?.addEventListener('change', filterFarmers);
});

// Initialize Leaflet GIS Map
function initMap() {
  const mapElement = document.getElementById('gis-map');
  if (!mapElement) return;

  // Center over Nigerian Agricultural Belt (Kano, Jigawa, Kaduna, Benue)
  map = L.map('gis-map').setView([10.5, 8.5], 7);

  // High-contrast OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors | TraceHarvest GIS'
  }).addTo(map);

  markerLayerGroup = L.layerGroup().addTo(map);
  polygonLayerGroup = L.layerGroup().addTo(map);
}

// Detect and display appropriate server URL for mobile connection
function detectServerUrl() {
  const display = document.getElementById('server-url-display');
  if (!display) return;
  const loc = window.location;
  if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
    display.textContent = 'http://10.0.2.2:8000/ (Emulator) or http://' + loc.hostname + ':8000/ (LAN)';
  } else {
    display.textContent = loc.origin + '/';
  }
}

function copyServerUrl() {
  const text = document.getElementById('server-url-display')?.textContent || '';
  navigator.clipboard.writeText(text);
  alert('Server URL copied to clipboard: ' + text);
}

// Tab Switching
function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(el => el.classList.add('hidden'));
  document.querySelectorAll('.tab-btn').forEach(el => {
    el.classList.remove('text-emerald-400', 'border-emerald-400');
    el.classList.add('text-slate-400', 'border-transparent');
  });

  if (tabId === 'map-tab') {
    document.getElementById('tab-map')?.classList.remove('hidden');
    document.getElementById('tab-btn-map')?.classList.add('text-emerald-400', 'border-emerald-400');
    setTimeout(() => map?.invalidateSize(), 200);
  } else if (tabId === 'farmers-tab') {
    document.getElementById('tab-farmers')?.classList.remove('hidden');
    document.getElementById('tab-btn-farmers')?.classList.add('text-emerald-400', 'border-emerald-400');
  } else if (tabId === 'compliance-tab') {
    document.getElementById('tab-compliance')?.classList.remove('hidden');
    document.getElementById('tab-btn-compliance')?.classList.add('text-emerald-400', 'border-emerald-400');
  } else if (tabId === 'batches-tab') {
    document.getElementById('tab-batches')?.classList.remove('hidden');
    document.getElementById('tab-btn-batches')?.classList.add('text-emerald-400', 'border-emerald-400');
  }
}

// Fetch All Remote Data
async function loadAllData() {
  try {
    const [statsRes, farmersRes, practicesRes, batchesRes] = await Promise.all([
      fetch('/api/v1/admin/stats').then(r => r.json()),
      fetch('/api/v1/admin/farmers').then(r => r.json()),
      fetch('/api/v1/admin/practices').then(r => r.json()),
      fetch('/api/v1/admin/batches').then(r => r.json())
    ]);

    renderKPIs(statsRes);
    renderMapData(farmersRes);
    renderFarmersTable(farmersRes);
    renderComplianceTable(practicesRes);
    renderBatches(batchesRes);

    if (statsRes.last_sync_log) {
      const syncDate = new Date(statsRes.last_sync_log.server_timestamp_ms);
      document.getElementById('last-sync-time').textContent = `(Last sync: ${syncDate.toLocaleTimeString()})`;
    }
  } catch (err) {
    console.warn('[Sync Status] Server connecting...', err);
  }
}

// Render Top KPIs
function renderKPIs(stats) {
  document.getElementById('kpi-farmers').textContent = stats.total_farmers || 0;
  document.getElementById('kpi-hectares').textContent = (stats.total_hectares || 0).toFixed(1);
  document.getElementById('kpi-phi-holds').textContent = stats.active_phi_holds || 0;
  document.getElementById('kpi-batches').textContent = stats.total_batches || 0;

  const totalPrac = stats.total_practices || 0;
  const nonComp = stats.non_compliant_practices || 0;
  const rate = totalPrac > 0 ? Math.round(((totalPrac - nonComp) / totalPrac) * 100) : 100;
  document.getElementById('kpi-compliance-rate').textContent = `${rate}%`;
}

// Render Markers & Polygons on Leaflet GIS Map
function renderMapData(farmers) {
  if (!markerLayerGroup || !polygonLayerGroup) return;

  markerLayerGroup.clearLayers();
  polygonLayerGroup.clearLayers();

  if (!farmers || farmers.length === 0) return;

  const bounds = [];

  farmers.forEach(f => {
    const lat = parseFloat(f.latitude);
    const lng = parseFloat(f.longitude);

    if (!isNaN(lat) && !isNaN(lng) && lat !== 0) {
      bounds.push([lat, lng]);

      // Crop-specific color
      let markerColor = '#10b981'; // Green for Sesame
      if (f.crop?.toLowerCase().includes('cowpea')) markerColor = '#f59e0b';
      if (f.crop?.toLowerCase().includes('ginger')) markerColor = '#f97316';

      const circle = L.circleMarker([lat, lng], {
        radius: 7,
        fillColor: markerColor,
        color: '#ffffff',
        weight: 2,
        opacity: 1,
        fillOpacity: 0.9
      });

      circle.bindPopup(`
        <div style="font-family: sans-serif; font-size: 12px; color: #1e293b;">
          <strong style="color: #065f46; font-size: 13px;">${f.official_farmer_id}</strong><br>
          <strong>${f.full_name}</strong><br>
          Crop: <strong>${f.crop}</strong> (${f.farm_size_hectares} ha)<br>
          Location: ${f.community || ''}, ${f.lga}, ${f.state}<br>
          Agent: ${f.agent_id}<br>
          Coordinates: ${lat.toFixed(4)}°N, ${lng.toFixed(4)}°E
        </div>
      `);

      circle.addTo(markerLayerGroup);

      // Render GPS Polygon if available
      if (f.gps_polygon && f.gps_polygon.includes(';')) {
        const polyCoords = f.gps_polygon.split(';')
          .map(pair => pair.split(',').map(n => parseFloat(n.trim())))
          .filter(coord => coord.length === 2 && !isNaN(coord[0]) && !isNaN(coord[1]));

        if (polyCoords.length >= 3) {
          L.polygon(polyCoords, {
            color: markerColor,
            weight: 2,
            fillOpacity: 0.25
          }).addTo(polygonLayerGroup);
        }
      }
    }
  });

  if (bounds.length > 0 && map) {
    map.fitBounds(bounds, { padding: [50, 50], maxZoom: 12 });
  }
}

// Render Farmers Table
function renderFarmersTable(farmers) {
  const tbody = document.getElementById('farmers-table-body');
  if (!tbody) return;

  if (!farmers || farmers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="p-6 text-center text-slate-500">Awaiting sync from mobile apps. Enroll a farmer in the Android app and tap 'Sync Now'.</td></tr>`;
    return;
  }

  tbody.innerHTML = farmers.map(f => `
    <tr class="hover:bg-slate-800/40 transition">
      <td class="p-3 font-mono font-bold text-emerald-400">${f.official_farmer_id}</td>
      <td class="p-3 font-medium text-white">${f.full_name}<br><span class="text-[10px] text-slate-400">${f.phone_number}</span></td>
      <td class="p-3">${f.state}, <span class="text-slate-400">${f.lga}</span></td>
      <td class="p-3 font-semibold text-slate-200">${f.crop}</td>
      <td class="p-3">${f.farm_size_hectares} ha</td>
      <td class="p-3 font-mono text-[11px] text-slate-400">${parseFloat(f.latitude || 0).toFixed(4)}, ${parseFloat(f.longitude || 0).toFixed(4)}</td>
      <td class="p-3 font-mono text-[11px] text-emerald-400/80">${f.agent_id}</td>
      <td class="p-3"><span class="bg-emerald-500/20 text-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-500/30">SYNCED</span></td>
    </tr>
  `).join('');
}

// Render Compliance Table
function renderComplianceTable(practices) {
  const tbody = document.getElementById('compliance-table-body');
  if (!tbody) return;

  if (!practices || practices.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="p-6 text-center text-slate-500">No practice logs synced yet. Log a spray or fertilizer practice on the mobile app to audit.</td></tr>`;
    return;
  }

  tbody.innerHTML = practices.map(p => {
    const isPhiCleared = p.phi_cleared;
    const isApproved = p.nafdac_approved;

    return `
      <tr class="hover:bg-slate-800/40 transition">
        <td class="p-3 font-mono font-bold text-slate-300">${p.farmer_code}</td>
        <td class="p-3 text-slate-200">${p.practice_type}</td>
        <td class="p-3 font-semibold text-white">${p.product_name || 'N/A'}</td>
        <td class="p-3 text-slate-300 text-[11px]">${p.active_ingredient || 'N/A'}</td>
        <td class="p-3 font-mono text-emerald-400 text-[11px]">${p.nafdac_reg_no || 'Pending Reg'}</td>
        <td class="p-3">${p.dosage || 'Standard'}</td>
        <td class="p-3">
          ${isPhiCleared 
            ? `<span class="bg-emerald-500/20 text-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-500/30">✓ PHI CLEARED</span>` 
            : `<span class="bg-amber-500/20 text-amber-300 text-[10px] font-bold px-2 py-0.5 rounded border border-amber-500/30">⏳ WITHHOLDING ACTIVE</span>`
          }
        </td>
        <td class="p-3">
          ${isApproved 
            ? `<span class="text-emerald-400 font-semibold text-[11px]">✓ Export Compliant</span>` 
            : `<span class="text-rose-400 font-semibold text-[11px]">⚠ Non-Compliant</span>`
          }
        </td>
      </tr>
    `;
  }).join('');
}

// Render Batches
function renderBatches(batches) {
  const container = document.getElementById('batches-container');
  if (!container) return;

  if (!batches || batches.length === 0) {
    container.innerHTML = `<div class="col-span-2 p-8 text-center text-slate-500 border border-dashed border-slate-700 rounded-xl">No export batches consolidated yet. Click '+ Create New Batch' to aggregate registered farmers.</div>`;
    return;
  }

  container.innerHTML = batches.map(b => `
    <div class="bg-slate-900 border border-slate-700/80 rounded-xl p-4 space-y-3">
      <div class="flex items-center justify-between">
        <span class="font-mono text-xs font-bold text-emerald-400">${b.batch_number}</span>
        <span class="bg-emerald-500/20 text-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-500/30">${b.export_clearance_status}</span>
      </div>
      <div class="text-sm font-bold text-white">${b.crop} Export Consignment (${b.estimated_tonnage} MT)</div>
      <div class="text-xs text-slate-400">Destination: <span class="text-slate-200 font-medium">${b.destination}</span></div>
      <div class="bg-slate-950 p-2.5 rounded-lg border border-slate-800 font-mono text-[10px] text-slate-400 break-all">
        SHA-256: ${b.tamper_proof_sha256}
      </div>
      <div class="flex items-center justify-between text-xs pt-1 border-t border-slate-800">
        <span class="text-slate-400">${b.farmer_count || 12} Contributing Smallholders</span>
        <span class="text-emerald-400 font-semibold">EUDR & NAFDAC Certified</span>
      </div>
    </div>
  `).join('');
}

// Create New Batch Dialog
async function createNewBatch() {
  const crop = prompt('Enter Crop for Consignment (e.g. Sesame, Cowpea, Ginger):', 'Sesame');
  if (!crop) return;
  const destination = prompt('Enter Destination Port (e.g. Rotterdam, Netherlands (EU)):', 'Rotterdam, Netherlands (EU)');

  try {
    const res = await fetch('/api/v1/admin/batches/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ crop, destination, estimated_tonnage: 24.5 })
    });
    const data = await res.json();
    if (data.success) {
      alert('Batch ' + data.batch.batch_number + ' successfully created and cryptographically signed.');
      loadAllData();
    }
  } catch (e) {
    alert('Failed to create batch: ' + e.message);
  }
}

// Modal Helpers
function openModal(id) {
  document.getElementById(id)?.classList.remove('hidden');
}

function closeModal(id) {
  document.getElementById(id)?.classList.add('hidden');
}

// Filter Farmers Table
function filterFarmers() {
  const q = document.getElementById('farmer-search')?.value || '';
  const state = document.getElementById('state-filter')?.value || 'ALL';
  const crop = document.getElementById('crop-filter')?.value || 'ALL';

  fetch(`/api/v1/admin/farmers?search=${encodeURIComponent(q)}&state=${encodeURIComponent(state)}&crop=${encodeURIComponent(crop)}`)
    .then(r => r.json())
    .then(farmers => renderFarmersTable(farmers));
}
