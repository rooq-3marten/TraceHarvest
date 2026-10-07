import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "TraceHarvest Central API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = ""
    
    # Database: Supports Vercel Postgres auto-injected POSTGRES_URL, standard DATABASE_URL, or SQLite fallback
    DATABASE_URL: str = (
        os.getenv("DATABASE_URL")
        or os.getenv("POSTGRES_URL")
        or os.getenv("POSTGRES_PRISMA_URL")
        or "sqlite:///./traceharvest.db"
    )
    
    # JWT Secrets
    SECRET_KEY: str = os.getenv("SECRET_KEY", "traceharvest_central_secret_key_nigeria_2026_change_in_prod")
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days for field agents in rural areas
    REFRESH_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30 # 30 days

    class Config:
        case_sensitive = True
        env_file = ".env"

settings = Settings()
