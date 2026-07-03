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

def create_token(user_id: str):
    payload = {"sub": user_id, "exp": datetime.utcnow() + timedelta(days=30)}
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

@router.post("/register")
async def register(data: RegisterData):
    existing = supabase.table("users").select("id").eq("telephone", data.telephone).execute()
    if existing.data:
        raise HTTPException(400, "Numéro déjà utilisé")
    hashed = hash_password(data.password)
    user = supabase.table("users").insert({
        "nom": data.nom,
        "telephone": data.telephone,
        "password": hashed,
        "solde": BONUS_BIENVENUE,  # bonus de bienvenue crédité une seule fois à l'inscription
        "referral_code": data.referral,
        "mon_code": data.telephone[-6:],
        "roue_fait": True  # la roue devient une simple animation, plus besoin de la proposer
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
