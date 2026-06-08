from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from config import supabase, SECRET_KEY, ALGORITHM
from passlib.hash import bcrypt
from jose import jwt
from datetime import datetime, timedelta

router = APIRouter(prefix="/auth", tags=["auth"])

class LoginData(BaseModel):
    telephone: str
    password: str

class RegisterData(BaseModel):
    nom: str
    telephone: str
    password: str
    referral: str = ""

def create_token(user_id: str):
    payload = {"sub": user_id, "exp": datetime.utcnow() + timedelta(days=30)}
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

@router.post("/register")
async def register(data: RegisterData):
    existing = supabase.table("users").select("id").eq("telephone", data.telephone).execute()
    if existing.data:
        raise HTTPException(400, "Numéro déjà utilisé")
    hashed = bcrypt.hash(data.password)
    user = supabase.table("users").insert({
        "nom": data.nom,
        "telephone": data.telephone,
        "password": hashed,
        "solde": 0,
        "referral_code": data.referral,
        "mon_code": data.telephone[-6:]
    }).execute()
    uid = user.data[0]["id"]
    return {"token": create_token(uid), "message": "Compte créé"}

@router.post("/login")
async def login(data: LoginData):
    user = supabase.table("users").select("*").eq("telephone", data.telephone).execute()
    if not user.data:
        raise HTTPException(400, "Numéro introuvable")
    u = user.data[0]
    if not bcrypt.verify(data.password, u["password"]):
        raise HTTPException(400, "Mot de passe incorrect")
    return {"token": create_token(u["id"]), "user": {"nom": u["nom"], "solde": u["solde"]}}
