from fastapi import FastAPI, Depends, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
from datetime import datetime, timedelta
from dotenv import load_dotenv

from config import supabase, SECRET_KEY, ALGORITHM

load_dotenv()

app = FastAPI(title="AEROWATT")

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

try:
    app.mount("/static", StaticFiles(directory="static"), name="static")
except:
    pass

from routers import auth
app.include_router(auth.router)

# ── PAGES HTML ──

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


# ── AUTHENTIFICATION API (centralisée, corrige les bugs de gestion d'erreurs) ──

security = HTTPBearer()

def get_current_user_id(credentials: HTTPAuthorizationCredentials = Depends(security)) -> str:
    """
    Décode le token JWT une seule fois, de façon cohérente pour toutes les routes.
    Corrige : JWTError non catché sur /api/depot, /api/retrait, /api/investir, /api/transactions
    (qui provoquait un crash 500 au lieu d'une erreur 401 propre).
    """
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        raise HTTPException(401, "Token invalide ou expiré")
    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(401, "Token invalide")
    return user_id

def get_current_user(user_id: str = Depends(get_current_user_id)) -> dict:
    """
    Récupère l'utilisateur complet et vérifie qu'il existe.
    Corrige : IndexError (500) sur /api/retrait et /api/investir quand l'utilisateur
    n'existe plus (compte supprimé, token orphelin, etc.)
    """
    result = supabase.table("users").select("*").eq("id", user_id).execute()
    if not result.data:
        raise HTTPException(404, "Utilisateur introuvable")
    return result.data[0]


# ── API ──

@app.get("/api/me")
async def get_me(user: dict = Depends(get_current_user)):
    return {
        "id": user["id"],
        "nom": user["nom"],
        "telephone": user["telephone"],
        "solde": user["solde"],
        "mon_code": user.get("mon_code", ""),
        "roue_fait": user.get("roue_fait", False)
    }

@app.get("/api/plans")
async def get_plans():
    plans = supabase.table("plans").select("*").order("prix").execute()
    return plans.data

@app.get("/api/config")
async def get_config():
    cfg = supabase.table("config").select("*").execute()
    return {row["cle"]: row["valeur"] for row in cfg.data}

@app.get("/api/transactions")
async def get_transactions(user_id: str = Depends(get_current_user_id)):
    txs = supabase.table("transactions").select("*").eq("user_id", user_id).order("created_at", desc=True).execute()
    return txs.data

@app.post("/api/depot")
async def demander_depot(data: dict, user_id: str = Depends(get_current_user_id)):
    montant = data.get("montant")
    if not montant or montant <= 0:
        raise HTTPException(400, "Montant invalide")
    tx = supabase.table("transactions").insert({
        "user_id": user_id,
        "type": "depot",
        "montant": montant,
        "methode": data.get("methode", ""),
        "numero_envoi": data.get("telephone", ""),
        "reference": data.get("reference", ""),
        "statut": "en_attente"
    }).execute()
    return {"message": "Dépôt soumis", "id": tx.data[0]["id"]}

@app.post("/api/retrait")
async def demander_retrait(data: dict, user: dict = Depends(get_current_user)):
    solde = user["solde"]
    montant = data.get("montant", 0)
    if not montant or montant <= 0:
        raise HTTPException(400, "Montant invalide")
    if montant > solde:
        raise HTTPException(400, "Solde insuffisant")

    # Condition : l'utilisateur doit avoir investi dans au moins un plan pour retirer
    invs = supabase.table("investments").select("id").eq("user_id", user["id"]).limit(1).execute()
    if not invs.data:
        raise HTTPException(400, "Vous devez investir dans un plan avant de pouvoir retirer")

    tx = supabase.table("transactions").insert({
        "user_id": user["id"],
        "type": "retrait",
        "montant": montant,
        "methode": data.get("methode", ""),
        "numero_envoi": data.get("telephone", ""),
        "statut": "en_attente"
    }).execute()
    return {"message": "Retrait demandé", "id": tx.data[0]["id"]}

@app.post("/api/investir")
async def investir(data: dict, user: dict = Depends(get_current_user)):
    solde = user["solde"]
    montant = data.get("montant", 0)
    if not montant or montant <= 0:
        raise HTTPException(400, "Montant invalide")
    if montant > solde:
        raise HTTPException(400, "Solde insuffisant")

    cycle_jours = data.get("cycle_jours", 20)
    date_debut = datetime.utcnow()
    date_fin = date_debut + timedelta(days=cycle_jours)

    supabase.table("investments").insert({
        "user_id": user["id"],
        "plan_id": data.get("plan_id", 0),
        "plan_nom": data.get("plan_nom", ""),
        "montant": montant,
        "revenu_quotidien": data.get("revenu_quotidien", 0),
        "revenu_total": data.get("revenu_total", 0),
        "cycle_jours": cycle_jours,
        "date_debut": date_debut.isoformat(),  # corrigé : manquait, cassait le calcul de progression côté frontend
        "date_fin": date_fin.isoformat(),
        "actif": True
    }).execute()

    nouveau_solde = solde - montant
    supabase.table("users").update({"solde": nouveau_solde}).eq("id", user["id"]).execute()

    supabase.table("transactions").insert({
        "user_id": user["id"],
        "type": "investissement",
        "montant": montant,
        "statut": "approuve",
        "note": data.get("plan_nom", "")
    }).execute()

    return {"message": "Investissement confirmé", "nouveau_solde": nouveau_solde}
