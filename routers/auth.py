from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from config import supabase, SECRET_KEY, ALGORITHM
from jose import jwt
from datetime import datetime, timedelta
import hashlib
import secrets

router = APIRouter(prefix="/auth", tags=["auth"])

BONUS_BIENVENUE = 1000  # XAF crédités automatiquement à l'inscription

class LoginData(BaseModel):
    telephone: str
    password: str

class RegisterData(BaseModel):
    nom: str
    telephone: str
    password: str
    referral: str = ""
    question_securite: str
    reponse_securite: str

class VerifierReponseData(BaseModel):
    telephone: str
    reponse: str

class ReinitialiserPasswordData(BaseModel):
    reset_token: str
    nouveau_mot_de_passe: str

def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    hashed = hashlib.sha256((password + salt).encode()).hexdigest()
    return f"{salt}:{hashed}"

def verify_password(password: str, stored: str) -> bool:
    try:
        salt, hashed = stored.split(":")
        return hashlib.sha256((password + salt).encode()).hexdigest() == hashed
    except:
        return False

def normaliser_reponse(reponse: str) -> str:
    # Insensible à la casse et aux espaces, pour éviter les échecs de comparaison
    # dus à une majuscule ou un espace en trop lors de la saisie.
    return reponse.strip().lower()

def create_token(user_id: str):
    payload = {"sub": user_id, "exp": datetime.utcnow() + timedelta(days=30)}
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

def create_reset_token(user_id: str):
    # Token à usage unique, valide 10 minutes, distinct du token de connexion normal
    # (le claim "purpose" empêche un token de connexion classique d'être utilisé pour réinitialiser le mot de passe).
    payload = {"sub": user_id, "purpose": "reset_password", "exp": datetime.utcnow() + timedelta(minutes=10)}
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

@router.post("/register")
async def register(data: RegisterData):
    existing = supabase.table("users").select("id").eq("telephone", data.telephone).execute()
    if existing.data:
        raise HTTPException(400, "Numéro déjà utilisé")

    # Résout le code de parrainage saisi vers le compte réel du parrain (s'il existe)
    parrain_id = None
    if data.referral:
        parrain = supabase.table("users").select("id").eq("mon_code", data.referral.strip()).execute()
        if parrain.data:
            parrain_id = parrain.data[0]["id"]

    hashed = hash_password(data.password)
    reponse_hashee = hash_password(normaliser_reponse(data.reponse_securite))
    user = supabase.table("users").insert({
        "nom": data.nom,
        "telephone": data.telephone,
        "password": hashed,
        "solde": BONUS_BIENVENUE,  # bonus de bienvenue crédité une seule fois à l'inscription
        "referral_code": data.referral,
        "parrain_id": parrain_id,
        "mon_code": data.telephone[-6:],
        "roue_fait": True,  # la roue devient une simple animation, plus besoin de la proposer
        "question_securite": data.question_securite,
        "reponse_securite": reponse_hashee
    }).execute()
    uid = user.data[0]["id"]
    return {"token": create_token(uid), "message": "Compte créé"}

@router.post("/login")
async def login(data: LoginData):
    user = supabase.table("users").select("*").eq("telephone", data.telephone).execute()
    if not user.data:
        raise HTTPException(400, "Numéro introuvable")
    u = user.data[0]
    if not verify_password(data.password, u["password"]):
        raise HTTPException(400, "Mot de passe incorrect")
    return {"token": create_token(u["id"]), "user": {"nom": u["nom"], "solde": u["solde"]}}


# ── MOT DE PASSE OUBLIÉ (via question de sécurité) ──

@router.get("/mot-de-passe-oublie/question")
async def obtenir_question(telephone: str):
    user = supabase.table("users").select("id, question_securite").eq("telephone", telephone).execute()
    if not user.data or not user.data[0].get("question_securite"):
        raise HTTPException(404, "Aucune question de sécurité trouvée pour ce numéro")
    return {"question_securite": user.data[0]["question_securite"]}

@router.post("/mot-de-passe-oublie/verifier")
async def verifier_reponse(data: VerifierReponseData):
    user = supabase.table("users").select("id, reponse_securite").eq("telephone", data.telephone).execute()
    if not user.data or not user.data[0].get("reponse_securite"):
        raise HTTPException(404, "Compte introuvable")
    u = user.data[0]
    if not verify_password(normaliser_reponse(data.reponse), u["reponse_securite"]):
        raise HTTPException(400, "Réponse incorrecte")
    return {"reset_token": create_reset_token(u["id"])}

@router.post("/mot-de-passe-oublie/reinitialiser")
async def reinitialiser_password(data: ReinitialiserPasswordData):
    try:
        payload = jwt.decode(data.reset_token, SECRET_KEY, algorithms=[ALGORITHM])
    except Exception:
        raise HTTPException(401, "Lien de réinitialisation invalide ou expiré")
    if payload.get("purpose") != "reset_password":
        raise HTTPException(401, "Token invalide")
    if len(data.nouveau_mot_de_passe) < 4:
        raise HTTPException(400, "Mot de passe trop court")
    nouveau_hash = hash_password(data.nouveau_mot_de_passe)
    supabase.table("users").update({"password": nouveau_hash}).eq("id", payload["sub"]).execute()
    return {"message": "Mot de passe réinitialisé avec succès"}
