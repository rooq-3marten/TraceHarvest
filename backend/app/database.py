import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.config import settings

db_url = settings.DATABASE_URL or "sqlite:///./traceharvest.db"

# Format fix for modern SQLAlchemy postgres scheme
if db_url.startswith("postgres://"):
    db_url = db_url.replace("postgres://", "postgresql://", 1)

# In serverless environments like Vercel (AWS Lambda), only /tmp is writable for local SQLite
if "sqlite" in db_url:
    # Check if working directory is writable
    if not os.access(".", os.W_OK) or os.getenv("VERCEL"):
        db_url = "sqlite:////tmp/traceharvest.db"
    connect_args = {"check_same_thread": False}
    engine = create_engine(db_url, connect_args=connect_args)
else:
    # PostgreSQL / Neon / Supabase serverless pooling
    engine = create_engine(
        db_url,
        pool_pre_ping=True,
        pool_recycle=300,
        pool_size=5,
        max_overflow=10
    )

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
