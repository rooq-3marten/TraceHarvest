// TraceHarvest Admin Portal - Interactive Logic & GIS Map Engine

let map;
let markerLayerGroup;
let polygonLayerGroup;

function escapeHtml(val) {
  if (val == null) return '';
  return String(val)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

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
  document.getElementById('kpi-compliance-rate').textContent =
    totalPrac > 0 ? `${Math.round(((totalPrac - nonComp) / totalPrac) * 100)}%` : '—';
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
          <strong style="color: #065f46; font-size: 13px;">${escapeHtml(f.official_farmer_id)}</strong><br>
          <strong>${escapeHtml(f.full_name)}</strong><br>
          Crop: <strong>${escapeHtml(f.crop)}</strong> (${escapeHtml(f.farm_size_hectares)} ha)<br>
          Location: ${escapeHtml(f.community || '')}, ${escapeHtml(f.lga)}, ${escapeHtml(f.state)}<br>
          Agent: ${escapeHtml(f.agent_id)}<br>
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
      <td class="p-3 font-mono font-bold text-emerald-400">${escapeHtml(f.official_farmer_id)}</td>
      <td class="p-3 font-medium text-white">${escapeHtml(f.full_name)}<br><span class="text-[10px] text-slate-400">${escapeHtml(f.phone_number)}</span></td>
      <td class="p-3">${escapeHtml(f.state)}, <span class="text-slate-400">${escapeHtml(f.lga)}</span></td>
      <td class="p-3 font-semibold text-slate-200">${escapeHtml(f.crop)}</td>
      <td class="p-3">${escapeHtml(f.farm_size_hectares)} ha</td>
      <td class="p-3 font-mono text-[11px] text-slate-400">${parseFloat(f.latitude || 0).toFixed(4)}, ${parseFloat(f.longitude || 0).toFixed(4)}</td>
      <td class="p-3 font-mono text-[11px] text-emerald-400/80">${escapeHtml(f.agent_id)}</td>
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
    const isPhiCleared = Boolean(p.phi_cleared);
    const isApproved = p.nafdac_approved === true && p.risk_level !== 'BANNED_MRL_VIOLATION';

    return `
      <tr class="hover:bg-slate-800/40 transition">
        <td class="p-3 font-mono font-bold text-slate-300">${escapeHtml(p.farmer_code)}</td>
        <td class="p-3 text-slate-200">${escapeHtml(p.practice_type)}</td>
        <td class="p-3 font-semibold text-white">${escapeHtml(p.product_name || 'N/A')}</td>
        <td class="p-3 text-slate-300 text-[11px]">${escapeHtml(p.active_ingredient || 'N/A')}</td>
        <td class="p-3 font-mono text-emerald-400 text-[11px]">${escapeHtml(p.nafdac_reg_no || '—')}</td>
        <td class="p-3">${escapeHtml(p.dosage || '—')}</td>
        <td class="p-3">
          ${isPhiCleared 
            ? `<span class="bg-emerald-500/20 text-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-500/30">✓ PHI ELAPSED</span>` 
            : `<span class="bg-amber-500/20 text-amber-300 text-[10px] font-bold px-2 py-0.5 rounded border border-amber-500/30">⏳ WITHHOLDING ACTIVE</span>`
          }
        </td>
        <td class="p-3">
          ${isApproved 
            ? `<span class="text-emerald-400 font-semibold text-[11px]">NAFDAC Registered</span>` 
            : `<span class="text-rose-400 font-semibold text-[11px]">⚠ Unregistered / Flagged</span>`
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
    container.innerHTML = `<div class="col-span-2 p-8 text-center text-slate-500 border border-dashed border-slate-700 rounded-xl">No batches consolidated yet. Sync batches from the mobile app or click '+ Create New Batch'.</div>`;
    return;
  }

  container.innerHTML = batches.map(b => `
    <div class="bg-slate-900 border border-slate-700/80 rounded-xl p-4 space-y-3">
      <div class="flex items-center justify-between">
        <span class="font-mono text-xs font-bold text-emerald-400">${escapeHtml(b.batch_number)}</span>
        <span class="bg-slate-800 text-slate-200 text-[10px] font-bold px-2 py-0.5 rounded border border-slate-700">${escapeHtml(b.export_clearance_status || 'RECORDED')}</span>
      </div>
      <div class="text-sm font-bold text-white">${escapeHtml(b.crop)} Consignment (${escapeHtml(b.estimated_tonnage)} MT)</div>
      <div class="text-xs text-slate-400">Destination: <span class="text-slate-200 font-medium">${escapeHtml(b.destination || '—')}</span></div>
      <div class="bg-slate-950 p-2.5 rounded-lg border border-slate-800 font-mono text-[10px] text-slate-400 break-all">
        SHA-256: ${escapeHtml(b.tamper_proof_sha256)}
      </div>
      <div class="flex items-center justify-between text-xs pt-1 border-t border-slate-800">
        <span class="text-slate-400">${Number.isFinite(Number(b.farmer_count)) ? Number(b.farmer_count) : 0} Contributing Smallholders</span>
        <span class="text-slate-400 font-mono text-[11px]">${escapeHtml(b.agent_id || 'Portal')}</span>
      </div>
    </div>
  `).join('');
}

// Create New Batch Dialog
async function createNewBatch() {
  const crop = prompt('Enter Crop for Consignment (e.g. Sesame, Cowpea, Ginger):', '');
  if (!crop || !crop.trim()) return;
  const destination = prompt('Enter Destination Port:', '');
  const tonnageRaw = prompt('Enter Tonnage (MT):', '');
  const tonnage = parseFloat(tonnageRaw) || 0;

  try {
    const res = await fetch('/api/v1/admin/batches/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ crop: crop.trim(), destination: (destination || '').trim(), estimated_tonnage: tonnage })
    });
    const data = await res.json();
    if (data.success) {
      alert('Batch ' + data.batch.batch_number + ' recorded.');
      loadAllData();
    } else if (data.error) {
      alert('Could not create batch: ' + data.error);
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
