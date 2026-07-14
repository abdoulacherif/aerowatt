"""
Intégration SoleasPay - Dépôt (Pay-In) uniquement.
Le retrait reste 100% manuel côté AEROWATT, on n'y touche pas.

⚠️ À FAIRE avant mise en prod :
- Mettre SOLEASPAY_API_KEY dans une variable d'environnement (jamais en dur dans le code versionné)
- Vérifier avec SoleasPay la liste exacte des pays couverts par MOMO / OM
- Tester en sandbox le format exact attendu pour "wallet" (numéro avec ou sans indicatif pays)
"""

import os
import time
import uuid
import requests

SOLEASPAY_BASE_URL = "https://soleaspay.com"

# ⚠️ Remplace par os.environ["SOLEASPAY_API_KEY"] en production.
SOLEASPAY_API_KEY = os.environ.get(
    "SOLEASPAY_API_KEY",
    "IVqhQEooplCcmUZqW16BuIjDcI-HqEnrFi0gYx82jJM-AP"
)

# Codes "service" fournis par SoleasPay (/api/services-list) — seuls les mobile money nous intéressent ici.
SERVICE_OM = 2     # Orange Money
SERVICE_MOMO = 1   # Tout le reste (MTN, Moov, Wave, T-Money, Airtel, M-Pesa...)


def map_operateur_to_service(operateur: str) -> int:
    """
    SoleasPay ne propose que 2 services mobile money génériques (MOMO / OM).
    On route Orange Money vers OM, tout le reste vers MOMO.
    ⚠️ Hypothèse à valider avec SoleasPay : ils détectent l'opérateur réel via le préfixe du numéro.
    """
    if "orange" in operateur.lower():
        return SERVICE_OM
    return SERVICE_MOMO


class SoleasPayError(Exception):
    def __init__(self, message: str, payload: dict | None = None):
        super().__init__(message)
        self.payload = payload or {}


def collect_payment(
    *,
    wallet: str,
    amount: float,
    currency: str,
    operateur: str,
    payer_name: str,
    payer_email: str | None = None,
    order_id: str | None = None,
    description: str = "Dépôt AEROWATT",
    success_url: str | None = None,
    failure_url: str | None = None,
) -> dict:
    """
    Initie un dépôt (Pay-In) via SoleasPay.
    Retourne le dict `data` de la réponse SoleasPay en cas de succès (status PROCESSING).
    Lève SoleasPayError sinon.
    """
    order_id = order_id or f"AEROWATT-{uuid.uuid4().hex[:12]}"
    service_id = map_operateur_to_service(operateur)

    headers = {
        "x-api-key": SOLEASPAY_API_KEY,
        "operation": "2",  # Pay-In
        "service": str(service_id),
        "Content-Type": "application/json",
    }

    body = {
        "wallet": wallet,
        "amount": amount,
        "currency": currency,
        "orderId": order_id,
        "description": description,
        "payer": payer_name,
        "payerEmail": payer_email or "",
        "successUrl": success_url or "",
        "failureUrl": failure_url or "",
    }

    resp = requests.post(
        f"{SOLEASPAY_BASE_URL}/api/agent/bills/v3",
        headers=headers,
        json=body,
        timeout=20,
    )

    try:
        data = resp.json()
    except ValueError:
        raise SoleasPayError(f"Réponse SoleasPay invalide (HTTP {resp.status_code})")

    if not data.get("success"):
        raise SoleasPayError(data.get("message", "Paiement refusé par SoleasPay"), payload=data)

    # data["data"] contient reference, external_reference, amount, currency, status...
    result = data["data"]
    result["order_id"] = order_id
    return result


def verify_payment(order_id: str, pay_id: str) -> dict:
    """
    Vérifie le statut d'un paiement via /api/agent/verif-pay.
    Utile en fallback si le callback webhook n'arrive pas.
    """
    headers = {"x-api-key": SOLEASPAY_API_KEY}
    resp = requests.get(
        f"{SOLEASPAY_BASE_URL}/api/agent/verif-pay",
        headers=headers,
        params={"orderId": order_id, "payId": pay_id},
        timeout=20,
    )
    try:
        data = resp.json()
    except ValueError:
        raise SoleasPayError(f"Réponse SoleasPay invalide (HTTP {resp.status_code})")

    if not data.get("success"):
        raise SoleasPayError(data.get("message", "Transaction introuvable"), payload=data)

    return data["data"]
