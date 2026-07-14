import os
import hashlib
import hmac

from fastapi import APIRouter, Request, HTTPException

from config import supabase

router = APIRouter(prefix="/wallet", tags=["wallet"])

# ⚠️ À REMPLACER : le secret que SoleasPay te fournit quand tu configures ton URL de callback
# (section "Callback" de leur doc — le header x-private-key est le hash SHA-512 de ce secret).
SOLEASPAY_CALLBACK_SECRET = os.environ.get("SOLEASPAY_CALLBACK_SECRET", "")


def _verify_callback_signature(received_key: str) -> bool:
    if not SOLEASPAY_CALLBACK_SECRET:
        return False
    expected = hashlib.sha512(SOLEASPAY_CALLBACK_SECRET.encode()).hexdigest()
    return hmac.compare_digest(expected, received_key or "")


@router.post("/webhooks/soleaspay")
async def soleaspay_callback(request: Request):
    """
    URL finale à déclarer dans le dashboard SoleasPay :
    https://tondomaine.com/wallet/webhooks/soleaspay
    """
    private_key = request.headers.get("x-private-key", "")
    if not _verify_callback_signature(private_key):
        raise HTTPException(401, "Signature invalide")

    payload = await request.json()
    success = payload.get("success")
    status = payload.get("status")  # SUCCESS | RECEIVED | REFUND
    data = payload.get("data", {})
    external_reference = data.get("external_reference")  # notre order_id, stocké dans transactions.reference

    if not external_reference:
        raise HTTPException(400, "external_reference manquant")

    tx = supabase.table("transactions").select("*").eq("reference", external_reference).execute()
    if not tx.data:
        raise HTTPException(404, "Transaction introuvable")
    transaction = tx.data[0]

    # Évite de créditer deux fois si SoleasPay renvoie le callback plusieurs fois
    if transaction["statut"] != "en_attente":
        return {"received": True, "note": "déjà traité"}

    if success and status == "SUCCESS":
        supabase.table("transactions").update({"statut": "approuve"}).eq("id", transaction["id"]).execute()
        user = supabase.table("users").select("solde").eq("id", transaction["user_id"]).execute()
        if user.data:
            nouveau_solde = (user.data[0]["solde"] or 0) + float(transaction["montant"])
            supabase.table("users").update({"solde": nouveau_solde}).eq("id", transaction["user_id"]).execute()
    else:
        supabase.table("transactions").update({"statut": "rejete"}).eq("id", transaction["id"]).execute()

    return {"received": True}