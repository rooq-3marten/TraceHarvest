# TraceHarvest Admin Web Portal & Sync Ingestion Engine

> **Centralized Oversight, GIS Provenance Mapping, and NAFDAC Export Compliance Sentinel for the TraceHarvest Android Field Agent Fleet.**

---

## 🌟 Overview

This package contains the complete, ready-to-deploy **TraceHarvest Admin Web Portal and Ingestion API**. It is built specifically to pair seamlessly with the **TraceHarvest Android Mobile App**.

### Key Capabilities:
1. **Upstream Sync Ingestion (`POST /api/v1/sync/upstream`)**:
   - Ingests offline farmer enrollments and field practice logs.
   - Enforces **Client UUID Idempotency** (network retries and unstable rural connections will never duplicate records).
   - Generates official cryptographic Farmer IDs (e.g. `TH-KAN-2026-XXXX`).
2. **Interactive GIS Farm Plot Viewer**:
   - Renders farm plot GPS polygons and centroids on an interactive satellite-backed map (Leaflet / OpenStreetMap).
   - Checks compliance with EU Deforestation Regulation (EUDR) coordinate requirements.
3. **NAFDAC Agrochemical & PHI Compliance Sentinel**:
   - Automatically cross-checks sprayed agrochemicals against NAFDAC-approved databases.
   - Tracks **Pre-Harvest Interval (PHI) count-down timers** to prevent premature harvesting before chemical residues degrade to safe export limits.
4. **Batch Aggregation & Provenance QR Passport Generator**:
   - Aggregates individual smallholder harvests into export shipping containers.
   - Generates digital traceability passports with SHA-256 tamper-evident checksums.
5. **Real-time Mobile Agent Activity**:
   - Monitors active field agents (e.g. `AGENT-NG-042`), sync timestamps, and regional coverage across Nigerian agricultural belts (Kano, Jigawa, Kaduna, Benue).

---

## 🚀 Quick Start (Run in 60 Seconds)

### Prerequisites:
- **Node.js 18+** installed on your computer or server.

### Installation & Launch:
```bash
# 1. Enter the project directory
cd traceharvest-admin-portal

# 2. Install dependencies
npm install

# 3. Start the server
npm start
```

The portal will immediately be live at:
👉 **`http://localhost:8000`** (or `http://0.0.0.0:8000`)

---

## 📱 How to Link the Mobile App to This Website

1. Ensure your server is accessible to the mobile device:
   - **Local Android Emulator**: Use `http://10.0.2.2:8000/`
   - **Physical Phone on same Wi-Fi**: Use `http://<YOUR_PC_LOCAL_IP>:8000/` (e.g., `http://192.168.1.50:8000/`)
   - **Production / Cloud (Render, Fly.io, Cloud Run)**: Use your public HTTPS domain (e.g., `https://traceharvest-admin.yourdomain.com/`)
2. Open the **TraceHarvest Mobile App** on Android.
3. Navigate to the **Sync Tab** (bottom navigation bar).
4. In the **Admin Website Connection** card, tap **Configure**.
5. Type or paste your server URL and tap **Save Endpoint**.
6. Tap **Sync Now**.
7. Watch your newly enrolled farmers and logged practices instantly populate on the Admin Website's GIS map and compliance dashboard!

---

## 📂 Project Structure

```
traceharvest-admin-portal/
├── README.md                      # This guide
├── SPECIFICATION.md               # Complete API and protocol specifications
├── mobile_integration_guide.md    # Step-by-step mobile app linking walkthrough
├── package.json                   # Node.js dependencies and run scripts
├── server.js                      # Express API server with SQLite / PostgreSQL fallback
├── schema.sql                     # Production PostgreSQL + PostGIS database schema
├── Dockerfile                     # Container deployment image
├── docker-compose.yml             # Docker stack with PostgreSQL
├── .env.example                   # Environment configuration variables
└── public/
    ├── index.html                 # Complete Admin Dashboard UI (Tailwind + Leaflet)
    ├── app.js                     # Frontend interactive logic, charts, and GIS map
    └── styles.css                 # Custom styling and branding
```

---

## ☁️ 1-Click Deployment Options

### Deploy with Docker:
```bash
docker-compose up -d
```

### Deploy to Render / Railway / Google Cloud Run:
- Build command: `npm install`
- Start command: `node server.js`
- Port: `8000` (or `$PORT`)
