import os
import sys

# Ensure root directory of backend is in the Python search path for Vercel
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

from app.main import app

# Vercel Serverless Function entry point
# Exports the FastAPI ASGI app instance as 'app'
