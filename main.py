from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import os

load_dotenv()

app = FastAPI(title="AEROWATT", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

from routers import auth, dashboard, investment, wallet
app.include_router(auth.router)
app.include_router(dashboard.router)
app.include_router(investment.router)
app.include_router(wallet.router)

@app.get("/")
async def root():
    from fastapi.responses import FileResponse
    return FileResponse("templates/splash.html")
