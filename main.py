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
# no-store empêche le navigateur (surtout Chrome Android) de garder une version en cache :
# chaque visite recharge le fichier réel depuis le serveur.
NO_CACHE_HEADERS = {
    "Cache-Control": "no-store, no-cache, must-revalidate, max-age=0",
    "Pragma": "no-cache",
    "Expires": "0"
}

def html_page(path: str):
    return FileResponse(path, headers=NO_CACHE_HEADERS)

@app.get("/")
async def root():
    return html_page("templates/splash.html")

@app.get("/dashboard")
async def dashboard():
    return html_page("templates/dashboard.html")

@app.get("/revenu")
async def revenu():
    return html_page("templates/revenu.html")

@app.get("/messages")
async def messages():
    return html_page("templates/messages.html")

@app.get("/actualites")
async def actualites():
    return html_page("templates/actualites.html")

@app.get("/equipe")
async def equipe():
    return html_page("templates/equipe.html")

@app.get("/profil")
async def profil():
    return html_page("templates/profil.html")

@app.get("/wallet")
async def wallet():
    return html_page("templates/wallet.html")

@app.get("/admin")
async def admin():
    return html_page("templates/admin.html")

@app.get('/preuves')
async def preuves():
    return html_page('templates/preuves.html')


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
    try:
        tx = supabase.table("transactions").insert({
            "user_id": user_id,
            "type": "depot",
            "montant": montant,
            "methode": data.get("methode", ""),
            "numero_envoi": data.get("telephone", ""),
            "reference": data.get("reference", ""),
            "pays": data.get("pays", ""),
            "statut": "en_attente"
        }).execute()
    except Exception as e:
        raise HTTPException(400, f"Erreur base de données : {str(e)}")
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

    try:
        tx = supabase.table("transactions").insert({
            "user_id": user["id"],
            "type": "retrait",
            "montant": montant,
            "methode": data.get("methode", ""),
            "numero_envoi": data.get("telephone", ""),
            "pays": data.get("pays", ""),
            "statut": "en_attente"
        }).execute()
    except Exception as e:
        raise HTTPException(400, f"Erreur base de données : {str(e)}")
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
        "date_debut": date_debut.isoformat(),
        "date_fin": date_fin.isoformat(),
        "type_versement": "fin_contrat",  # les plans de /dashboard versent en une fois à la fin du cycle
        "jours_verses": 0,
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


# ── VERSEMENT AUTOMATIQUE DES GAINS (appelé chaque jour par un cron Vercel) ──

import os

@app.get("/api/cron/verser-revenus")
async def verser_revenus(secret: str = ""):
    """
    Parcourt tous les investissements actifs et verse les gains :
    - type_versement='quotidien' (plans de /revenu) : crédite le revenu du jour,
      pour chaque jour écoulé depuis le dernier versement, jusqu'à la fin du cycle.
    - type_versement='fin_contrat' (plans de /dashboard) : crédite le revenu total
      en une seule fois, une fois le cycle terminé.
    Protégé par un secret (variable d'env CRON_SECRET) pour empêcher un déclenchement
    public non autorisé qui créditerait les comptes en boucle.
    """
    cron_secret = os.getenv("CRON_SECRET", "")
    if cron_secret and secret != cron_secret:
        raise HTTPException(403, "Non autorisé")

    now = datetime.utcnow()
    resultats = {"quotidien_verses": 0, "fin_contrat_verses": 0, "erreurs": []}

    # ── Plans à versement quotidien ──
    quotidiens = supabase.table("investments").select("*").eq("actif", True).eq("type_versement", "quotidien").execute()
    for inv in quotidiens.data:
        try:
            date_debut = datetime.fromisoformat(inv["date_debut"].replace("Z", "+00:00")).replace(tzinfo=None)
            cycle_jours = inv["cycle_jours"]
            jours_deja_verses = inv.get("jours_verses", 0)
            jours_dus = min((now.date() - date_debut.date()).days, cycle_jours)

            if jours_dus > jours_deja_verses:
                nb_jours_a_payer = jours_dus - jours_deja_verses
                gain = nb_jours_a_payer * inv["revenu_quotidien"]

                user = supabase.table("users").select("solde").eq("id", inv["user_id"]).execute()
                if not user.data:
                    continue
                nouveau_solde = (user.data[0]["solde"] or 0) + gain
                supabase.table("users").update({"solde": nouveau_solde}).eq("id", inv["user_id"]).execute()

                termine = jours_dus >= cycle_jours
                supabase.table("investments").update({
                    "jours_verses": jours_dus,
                    "dernier_versement": now.isoformat(),
                    "actif": not termine
                }).eq("id", inv["id"]).execute()

                supabase.table("transactions").insert({
                    "user_id": inv["user_id"],
                    "type": "revenu_quotidien",
                    "montant": gain,
                    "statut": "approuve",
                    "note": inv.get("plan_nom", "")
                }).execute()

                resultats["quotidien_verses"] += 1
        except Exception as e:
            resultats["erreurs"].append(f"investment {inv.get('id')}: {str(e)}")

    # ── Plans à versement en fin de contrat ──
    fins_contrat = supabase.table("investments").select("*").eq("actif", True).eq("type_versement", "fin_contrat").execute()
    for inv in fins_contrat.data:
        try:
            date_fin = datetime.fromisoformat(inv["date_fin"].replace("Z", "+00:00")).replace(tzinfo=None)
            if now >= date_fin:
                gain = inv["revenu_total"]

                user = supabase.table("users").select("solde").eq("id", inv["user_id"]).execute()
                if not user.data:
                    continue
                nouveau_solde = (user.data[0]["solde"] or 0) + gain
                supabase.table("users").update({"solde": nouveau_solde}).eq("id", inv["user_id"]).execute()

                supabase.table("investments").update({
                    "actif": False,
                    "dernier_versement": now.isoformat()
                }).eq("id", inv["id"]).execute()

                supabase.table("transactions").insert({
                    "user_id": inv["user_id"],
                    "type": "revenu_fin_contrat",
                    "montant": gain,
                    "statut": "approuve",
                    "note": inv.get("plan_nom", "")
                }).execute()

                resultats["fin_contrat_verses"] += 1
        except Exception as e:
            resultats["erreurs"].append(f"investment {inv.get('id')}: {str(e)}")

    return resultats


# ── PARRAINAGE ──

@app.post("/api/parrainage/lier")
async def lier_parrainage(data: dict, user: dict = Depends(get_current_user)):
    """
    Permet à un utilisateur d'ajouter un code de parrainage après son inscription,
    depuis son profil. Refusé si un parrain est déjà défini, si le code n'existe pas,
    ou si l'utilisateur essaie de se parrainer lui-même.
    """
    if user.get("parrain_id"):
        raise HTTPException(400, "Vous avez déjà un parrain, impossible d'en changer")

    code = (data.get("code") or "").strip()
    if not code:
        raise HTTPException(400, "Code de parrainage requis")

    parrain = supabase.table("users").select("id, nom").eq("mon_code", code).execute()
    if not parrain.data:
        raise HTTPException(404, "Code de parrainage introuvable")

    parrain_id = parrain.data[0]["id"]
    if parrain_id == user["id"]:
        raise HTTPException(400, "Vous ne pouvez pas utiliser votre propre code")

    supabase.table("users").update({"parrain_id": parrain_id}).eq("id", user["id"]).execute()
    return {"message": "Parrain lié avec succès", "parrain_nom": parrain.data[0]["nom"]}


@app.get("/api/equipe")
async def get_equipe(user: dict = Depends(get_current_user)):
    """
    Retourne le code de parrainage réel de l'utilisateur, ses filleuls sur 3 niveaux,
    et le détail de ses commissions gagnées (calculées à partir des transactions
    de type commission_n1/n2/n3 qui lui sont créditées).
    """
    def get_filleuls(parent_ids):
        if not parent_ids:
            return []
        res = supabase.table("users").select("id, nom, telephone, created_at").in_("parrain_id", parent_ids).execute()
        return res.data or []

    niveau1 = get_filleuls([user["id"]])
    ids_n1 = [f["id"] for f in niveau1]
    niveau2 = get_filleuls(ids_n1)
    ids_n2 = [f["id"] for f in niveau2]
    niveau3 = get_filleuls(ids_n2)

    # Détermine qui est "actif" (au moins un investissement) parmi tous les filleuls
    tous_ids = ids_n1 + [f["id"] for f in niveau2] + [f["id"] for f in niveau3]
    actifs_ids = set()
    if tous_ids:
        invs = supabase.table("investments").select("user_id").in_("user_id", tous_ids).execute()
        actifs_ids = {i["user_id"] for i in (invs.data or [])}

    def format_filleul(f, niveau):
        return {
            "nom": f["nom"],
            "telephone": f["telephone"],
            "niveau": niveau,
            "actif": f["id"] in actifs_ids,
            "date": f["created_at"]
        }

    filleuls = (
        [format_filleul(f, 1) for f in niveau1] +
        [format_filleul(f, 2) for f in niveau2] +
        [format_filleul(f, 3) for f in niveau3]
    )

    # Commissions gagnées par niveau
    commissions_tx = supabase.table("transactions").select("type, montant").eq("user_id", user["id"]).in_(
        "type", ["commission_n1", "commission_n2", "commission_n3"]
    ).execute()
    gains_n1 = sum(t["montant"] for t in (commissions_tx.data or []) if t["type"] == "commission_n1")
    gains_n2 = sum(t["montant"] for t in (commissions_tx.data or []) if t["type"] == "commission_n2")
    gains_n3 = sum(t["montant"] for t in (commissions_tx.data or []) if t["type"] == "commission_n3")

    cfg = supabase.table("config").select("*").execute()
    cfg_map = {row["cle"]: row["valeur"] for row in (cfg.data or [])}

    return {
        "mon_code": user.get("mon_code", ""),
        "a_deja_un_parrain": bool(user.get("parrain_id")),
        "commissions": {
            "n1": cfg_map.get("commission_n1", "20"),
            "n2": cfg_map.get("commission_n2", "8"),
            "n3": cfg_map.get("commission_n3", "3")
        },
        "gains": {"n1": gains_n1, "n2": gains_n2, "n3": gains_n3, "total": gains_n1 + gains_n2 + gains_n3},
        "filleuls": filleuls
    }
