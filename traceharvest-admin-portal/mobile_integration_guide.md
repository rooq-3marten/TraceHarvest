# Mobile App Integration Guide

Follow these simple steps to link the **TraceHarvest Android Mobile App** with your **Admin Web Portal**.

---

### Step 1: Start Your Admin Web Server

Open your terminal, go into the `traceharvest-admin-portal` folder, and start the server:

```bash
npm install
npm start
```
You should see:
```
✓ TraceHarvest Admin Server listening on port 8000
✓ Web Dashboard: http://localhost:8000
✓ Upstream Sync: http://localhost:8000/api/v1/sync/upstream
```

---

### Step 2: Determine Your Server URL

Depending on where you are running the Android mobile app, choose the matching URL:

| Where you run Android App | Where your Admin Server is running | What URL to use in Android App |
|---|---|---|
| **Android Emulator** | On the same computer (localhost) | `http://10.0.2.2:8000/` |
| **Physical Android Phone** | On your computer on the same Wi-Fi | `http://<YOUR_COMPUTER_IP>:8000/` *(e.g. `http://192.168.1.55:8000/`)* |
| **Cloud Deployment** | Render / Railway / Cloud Run / VPS | `https://your-admin-service.onrender.com/` |

> **Tip for Physical Phones:** Run `ipconfig` (Windows) or `ifconfig` / `ip a` (Mac/Linux) to find your local IPv4 address. Make sure your computer's firewall allows incoming connections on port 8000.

---

### Step 3: Configure the Mobile App

1. Launch **TraceHarvest** on your Android device.
2. Tap the **Sync Tab** in the bottom navigation.
3. Locate the card titled **ADMIN WEBSITE CONNECTION**:
   - Tap **"Configure"**.
   - In the **Admin API Base URL** field, type your server URL (e.g. `http://10.0.2.2:8000/` or `https://your-domain.com/`).
   - Tap **"Save Endpoint"**.
   - You will see the confirmation message: `✓ Target URL updated`.

---

### Step 4: Perform a Live Test

1. Tap the **Enroll Tab** in the mobile app and register a new farmer with GPS coordinates.
2. Tap the **Practices Tab** and record an application (e.g. Pesticide or Fertilizer).
3. Return to the **Sync Tab**. You will see:
   `Record(s) Stored Locally: 1 Farmer, 1 Practice`
4. Tap **"Sync Now"**.
5. Switch to your computer browser at `http://localhost:8000`.
6. Watch the newly synced farmer immediately appear:
   - On the **GIS Satellite Map** with GPS boundary and centroid.
   - In the **Live Synced Farmers Registry** with an assigned official ID.
   - In the **Agrochemical PHI Compliance Tracker**.
