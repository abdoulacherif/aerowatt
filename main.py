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

@app.get('/preuves')
async def preuves():
    return FileResponse('templates/preuves.html')

from fastapi import Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError

security = HTTPBearer()

@app.get("/api/me")
async def get_me(credentials: HTTPAuthorizationCredentials = Depends(security)):
    from config import supabase, SECRET_KEY, ALGORITHM
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
        user_id = payload.get("sub")
        user = supabase.table("users").select("*").eq("id", user_id).execute()
        if not user.data:
            raise HTTPException(404, "Utilisateur introuvable")
        u = user.data[0]
        return {
            "id": u["id"],
            "nom": u["nom"],
            "telephone": u["telephone"],
            "solde": u["solde"],
            "mon_code": u.get("mon_code",""),
            "roue_fait": u.get("roue_fait", False)
        }
    except JWTError:
        raise HTTPException(401, "Token invalide")

@app.get("/api/plans")
async def get_plans():
    from config import supabase
    plans = supabase.table("plans").select("*").order("prix").execute()
    return plans.data

@app.get("/api/config")
async def get_config():
    from config import supabase
    cfg = supabase.table("config").select("*").execute()
    return {row["cle"]: row["valeur"] for row in cfg.data}

@app.get("/api/transactions")
async def get_transactions(credentials: HTTPAuthorizationCredentials = Depends(security)):
    from config import supabase, SECRET_KEY, ALGORITHM
    payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    user_id = payload.get("sub")
    txs = supabase.table("transactions").select("*").eq("user_id", user_id).order("created_at", desc=True).execute()
    return txs.data

@app.post("/api/depot")
async def demander_depot(data: dict, credentials: HTTPAuthorizationCredentials = Depends(security)):
    from config import supabase, SECRET_KEY, ALGORITHM
    payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    user_id = payload.get("sub")
    tx = supabase.table("transactions").insert({
        "user_id": user_id,
        "type": "depot",
        "montant": data.get("montant"),
        "methode": data.get("methode",""),
        "numero_envoi": data.get("telephone",""),
        "reference": data.get("reference",""),
        "statut": "en_attente"
    }).execute()
    return {"message": "Dépôt soumis", "id": tx.data[0]["id"]}

@app.post("/api/retrait")
async def demander_retrait(data: dict, credentials: HTTPAuthorizationCredentials = Depends(security)):
    from config import supabase, SECRET_KEY, ALGORITHM
    payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    user_id = payload.get("sub")
    user = supabase.table("users").select("solde").eq("id", user_id).execute()
    solde = user.data[0]["solde"]
    montant = data.get("montant", 0)
    if montant > solde:
        raise HTTPException(400, "Solde insuffisant")
    tx = supabase.table("transactions").insert({
        "user_id": user_id,
        "type": "retrait",
        "montant": montant,
        "methode": data.get("methode",""),
        "numero_envoi": data.get("telephone",""),
        "statut": "en_attente"
    }).execute()
    return {"message": "Retrait demandé", "id": tx.data[0]["id"]}
