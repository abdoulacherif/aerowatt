package com.locafric.shared

// Représente un bien immobilier (maison, chambre, appartement)
data class Bien(
    val id: String,
    val pays: String,
    val ville: String,
    val quartier: String,
    val type: String,       // ex: "chambre simple", "chambre salon", "appartement"
    val capacite: Int,      // nombre de personnes
    val loyer: Double,
    val disponible: Boolean
)