from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="AEROWATT")

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

try:
    app.mount("/static", StaticFiles(directory="static"), name="static")
except:
    pass

from routers import auth
app.include_router(auth.router)

@app.get("/")
async def root():
    return FileResponse("templates/splash.html")

@app.get("/dashboard")
async def dashboard():
    return FileResponse("templates/dashboard.html")

@app.get("/revenu")
async def revenu():
    return FileResponse("templates/revenu.html")

@app.get("/messages")
async def messages():
    return FileResponse("templates/messages.html")

@app.get("/actualites")
async def actualites():
    return FileResponse("templates/actualites.html")

@app.get("/equipe")
async def equipe():
    return FileResponse("templates/equipe.html")

@app.get("/profil")
async def profil():
    return FileResponse("templates/profil.html")

@app.get("/wallet")
async def wallet():
    return FileResponse("templates/wallet.html")

@app.get("/admin")
async def admin():
    return FileResponse("templates/admin.html")
