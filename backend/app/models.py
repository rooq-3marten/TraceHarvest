from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Table, Text
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.database import Base

def now_utc():
    return datetime.now(timezone.utc)

# 1. Join Tables
batch_farmers = Table(
    "batch_farmers",
    Base.metadata,
    Column("batch_id", String(64), ForeignKey("batches.id", ondelete="CASCADE"), primary_key=True),
    Column("farmer_id", String(64), ForeignKey("farmers.id", ondelete="CASCADE"), primary_key=True),
)

export_batches = Table(
    "export_batches",
    Base.metadata,
    Column("export_id", String(64), ForeignKey("exports.id", ondelete="CASCADE"), primary_key=True),
    Column("batch_id", String(64), ForeignKey("batches.id", ondelete="CASCADE"), primary_key=True),
)

# 2. User Model (Admin, Ops, Support, Analysts)
class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    email = Column(String(255), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False, default="admin") # admin, ops, support, analyst
    full_name = Column(String(150), nullable=True)
    created_at = Column(DateTime(timezone=True), default=now_utc)

# 3. Field Agent Model
class Agent(Base):
    __tablename__ = "agents"

    id = Column(String(64), primary_key=True) # e.g. AGENT-NG-042
    name = Column(String(150), nullable=False)
    phone = Column(String(30), nullable=False)
    email = Column(String(255), nullable=True)
    state = Column(String(50), nullable=False)
    cooperative = Column(String(150), nullable=True)
    status = Column(String(30), nullable=False, default="ACTIVE") # ACTIVE, OFFLINE, SUSPENDED
    device_id = Column(String(100), nullable=True)
    last_sync_at = Column(DateTime(timezone=True), nullable=True)

    farmers = relationship("Farmer", back_populates="agent")
    practices = relationship("PracticeLog", back_populates="agent")
    batches = relationship("Batch", back_populates="agent")
    sync_events = relationship("SyncEvent", back_populates="agent")

# 4. Farmer Model
class Farmer(Base):
    __tablename__ = "farmers"

    id = Column(String(64), primary_key=True) # localId or server ID
    farmer_code = Column(String(32), unique=True, nullable=False, index=True) # e.g. TH-KAN-2026-1048
    name = Column(String(150), nullable=False)
    phone = Column(String(30), nullable=False)
    gps_lat = Column(Float, nullable=True)
    gps_lng = Column(Float, nullable=True)
    crop_type = Column(String(50), nullable=False)
    cooperative = Column(String(150), nullable=True)
    enrolled_by_agent_id = Column(String(64), ForeignKey("agents.id"), nullable=True)
    source = Column(String(30), nullable=False, default="agent") # agent, ussd, web
    created_at = Column(DateTime(timezone=True), default=now_utc)
    synced_at = Column(DateTime(timezone=True), default=now_utc)

    agent = relationship("Agent", back_populates="farmers")
    practices = relationship("PracticeLog", back_populates="farmer", cascade="all, delete-orphan")
    batches = relationship("Batch", secondary=batch_farmers, back_populates="farmers")

# 5. Practice Log Model
class PracticeLog(Base):
    __tablename__ = "practice_logs"

    id = Column(String(64), primary_key=True)
    farmer_id = Column(String(64), ForeignKey("farmers.id", ondelete="CASCADE"), nullable=False)
    practice_type = Column(String(100), nullable=False)
    product_name = Column(String(150), nullable=True)
    quantity = Column(Float, default=1.0)
    log_date = Column(DateTime(timezone=True), nullable=False, default=now_utc)
    source = Column(String(30), nullable=False, default="agent") # agent, ussd
    agent_id = Column(String(64), ForeignKey("agents.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=now_utc)
    synced_at = Column(DateTime(timezone=True), default=now_utc)

    farmer = relationship("Farmer", back_populates="practices")
    agent = relationship("Agent", back_populates="practices")

# 6. Consignment Batch Model
class Batch(Base):
    __tablename__ = "batches"

    id = Column(String(64), primary_key=True)
    batch_code = Column(String(64), unique=True, nullable=False, index=True)
    agent_id = Column(String(64), ForeignKey("agents.id"), nullable=True)
    total_quantity = Column(Float, nullable=False, default=0.0)
    quality_grade = Column(String(50), nullable=False, default="Grade A Export Ready")
    aggregation_gps_lat = Column(Float, nullable=True)
    aggregation_gps_lng = Column(Float, nullable=True)
    created_at = Column(DateTime(timezone=True), default=now_utc)
    synced_at = Column(DateTime(timezone=True), default=now_utc)

    agent = relationship("Agent", back_populates="batches")
    farmers = relationship("Farmer", secondary=batch_farmers, back_populates="batches")
    exports = relationship("Export", secondary=export_batches, back_populates="batches")

# 7. Export Shipment Model
class Export(Base):
    __tablename__ = "exports"

    id = Column(String(64), primary_key=True)
    shipment_code = Column(String(64), unique=True, nullable=False, index=True)
    exporter_id = Column(String(64), nullable=False)
    destination = Column(String(150), nullable=False)
    status = Column(String(50), nullable=False, default="PENDING_CLEARANCE")
    created_at = Column(DateTime(timezone=True), default=now_utc)

    batches = relationship("Batch", secondary=export_batches, back_populates="exports")

# 8. Sync Event Audit Trail Model
class SyncEvent(Base):
    __tablename__ = "sync_events"

    id = Column(Integer, primary_key=True, autoincrement=True)
    agent_id = Column(String(64), ForeignKey("agents.id"), nullable=True)
    device_id = Column(String(100), nullable=True)
    event_type = Column(String(50), nullable=False) # PUSH, PULL, HEARTBEAT
    entity_type = Column(String(50), nullable=False) # FARMER, PRACTICE_LOG, BATCH, BULK
    entity_id = Column(String(64), nullable=True)
    status = Column(String(30), nullable=False) # success, failed, pending
    error_message = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), default=now_utc)

    agent = relationship("Agent", back_populates="sync_events")
