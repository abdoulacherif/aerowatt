"""
Routes FastAPI : dépôt automatique via SoleasPay + webhook de confirmation.
À brancher sur ton app existante (comme WIN AFFINITY) :
    from routes_depot import router as depot_router
    app.include_router(depot_router)

⚠️ À adapter à ton schéma réel : le nom des tables Supabase, la fonction
get_current_user (dépendance d'auth), et la façon dont tu crédites le solde.
"""

import os
import hashlib
import hmac

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel

from soleaspay_service import collect_payment, SoleasPayError

# ⚠️ Remplace cet import par ta vraie dépendance d'auth existante (ex: from auth import get_current_user)
from auth import get_current_user  # noqa: E402

router = APIRouter()

# ⚠️ À remplacer : le secret que SoleasPay te donne quand tu configures ton URL de callback
# (voir section "Callback" de leur doc — hash 512 du secret).
SOLEASPAY_CALLBACK_SECRET = os.environ.get("SOLEASPAY_CALLBACK_SECRET", "")


class DepotRequest(BaseModel):
    montant: float
    methode: str          # ex: "Orange Money", "MTN Mobile Money"...
    pays: str
    telephone: str        # numéro Mobile Money du client (paramètre "wallet")
    devise: str = "XAF"


@router.post("/api/depot")
async def demander_depot(payload: DepotRequest, user=Depends(get_current_user)):
    """
    ⚠️ Remplace `get_current_user` par ta vraie dépendance d'authentification
    (celle qui vérifie le Bearer Token de l'utilisateur AEROWATT, pas SoleasPay).
    """
    try:
        result = collect_payment(
            wallet=payload.telephone,
            amount=payload.montant,
            currency=payload.devise,
            operateur=payload.methode,
            payer_name=user.get("nom", "Client AEROWATT"),
            payer_email=user.get("email"),
        )
    except SoleasPayError as e:
        raise HTTPException(status_code=400, detail=str(e))

    # ⚠️ Adapte au nom réel de ta table / colonnes (voir hist-list dans wallet.html : type, methode, statut, montant, created_at)
    # supabase.table("transactions").insert({
    #     "user_id": user["id"],
    #     "type": "depot",
    #     "methode": payload.methode,
    #     "pays": payload.pays,
    #     "montant": payload.montant,
    #     "statut": "en_attente",
    #     "reference": result["reference"],
    #     "external_reference": result["order_id"],
    # }).execute()

    return {
        "success": True,
        "reference": result["reference"],
        "order_id": result["order_id"],
        "status": result.get("status", "PROCESSING"),
        "message": "Dépôt initié, en attente de confirmation du client sur son téléphone.",
    }


def _verify_callback_signature(raw_body: bytes, received_key: str) -> bool:
    """
    SoleasPay envoie x-private-key = hash SHA-512 de ton secret de callback.
    Compare en constant-time pour éviter les attaques par timing.
    """
    if not SOLEASPAY_CALLBACK_SECRET:
        return False
    expected = hashlib.sha512(SOLEASPAY_CALLBACK_SECRET.encode()).hexdigest()
    return hmac.compare_digest(expected, received_key or "")


@router.post("/api/webhooks/soleaspay")
async def soleaspay_callback(request: Request):
    private_key = request.headers.get("x-private-key", "")
    raw_body = await request.body()

    if not _verify_callback_signature(raw_body, private_key):
        raise HTTPException(status_code=401, detail="Signature invalide")

    payload = await request.json()
    success = payload.get("success")
    status = payload.get("status")  # SUCCESS | RECEIVED | REFUND
    data = payload.get("data", {})
    external_reference = data.get("external_reference")  # ton order_id
    internal_reference = data.get("reference")            # ref SoleasPay (MLS...)
    amount = data.get("amount")

    # ⚠️ Ici : retrouver la transaction via external_reference (ou internal_reference),
    # mettre à jour son statut, et créditer le solde utilisateur SEULEMENT si status == "SUCCESS".
    #
    # tx = supabase.table("transactions").select("*").eq("external_reference", external_reference).single().execute()
    # if success and status == "SUCCESS":
    #     supabase.table("transactions").update({"statut": "approuve"}).eq("id", tx.data["id"]).execute()
    #     supabase.rpc("crediter_solde", {"user_id": tx.data["user_id"], "montant": amount}).execute()
    # else:
    #     supabase.table("transactions").update({"statut": "rejete"}).eq("id", tx.data["id"]).execute()

    return {"received": True}
