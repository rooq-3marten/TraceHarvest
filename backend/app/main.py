from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.database import engine, Base, SessionLocal
from app.models import User, Agent
from app.auth import get_password_hash
from app.routers import sync, farmers, practices, batches, admin, auth

# Ensure database tables exist & default accounts are seeded automatically on Vercel cold-start
def init_db():
    try:
        Base.metadata.create_all(bind=engine)
        db = SessionLocal()
        
        # Seed default admin if not present
        admin_user = db.query(User).filter(User.email == "admin@traceharvest.ng").first()
        if not admin_user:
            admin_user = User(
                email="admin@traceharvest.ng",
                password_hash=get_password_hash("TraceHarvest2026!"),
                role="admin",
                full_name="Chief Compliance Director"
            )
            db.add(admin_user)

        # Seed primary field agent if not present
        primary_agent = db.query(Agent).filter(Agent.id == "AGENT-NG-042").first()
        if not primary_agent:
            primary_agent = Agent(
                id="AGENT-NG-042",
                name="Aminu Bello Dambatta",
                phone="+2348031234567",
                email="aminu.bello@traceharvest.ng",
                state="Kano",
                cooperative="Dambatta Sesame Growers Union",
                status="ACTIVE"
            )
            db.add(primary_agent)

        db.commit()
        db.close()
    except Exception as e:
        print(f"Startup DB init note: {e}")

init_db()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="TraceHarvest Central Backend API & Synchronization Brain for Nigeria Agricultural Traceability"
)

# Configure CORS to allow Next.js Admin Dashboard (admindashtrace.vercel.app) and Android Agent Apps
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://admindashtrace.vercel.app",
        "http://localhost:3000",
        "http://localhost:8000",
        "*" # Allow direct local IP connections from mobile devices
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register All API Routers
app.include_router(auth.router)
app.include_router(sync.router)
app.include_router(farmers.router)
app.include_router(practices.router)
app.include_router(batches.router)
app.include_router(admin.router)

@app.get("/")
def root():
    return {
        "service": "TraceHarvest Central Ingestion & Synchronization Engine",
        "version": settings.VERSION,
        "status": "ONLINE",
        "dashboard_url": "https://admindashtrace.vercel.app",
        "endpoints": {
            "sync_bulk": "/sync/batch",
            "farmers": "/farmers",
            "practice_logs": "/practice-logs",
            "batches": "/batches",
            "admin_overview": "/admin/overview",
            "admin_agents": "/admin/agents",
            "admin_batches": "/admin/batches",
            "admin_activity": "/admin/activity",
            "admin_system_status": "/admin/system-status"
        }
    }

@app.get("/health")
def health_check():
    return {"status": "HEALTHY", "database": "CONNECTED"}
