package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StatutPaiement { PAYE, RETARD }

data class BienBailleur(
    val id: String,
    val nom: String,
    val statutPaiement: StatutPaiement
)

data class ResumeBailleur(
    val nomBailleur: String,
    val nombreBiens: Int,
    val revenusMois: String,
    val nombreRetards: Int,
    val biens: List<BienBailleur>
)

@Composable
fun EcranTableauBordBailleur(
    resume: ResumeBailleur,
    onOuvrirBien: (BienBailleur) -> Unit,
    onAjouterBien: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .padding(16.dp)
    ) {
        Text(
            "Bonjour ${resume.nomBailleur}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "${resume.nombreBiens} biens gérés",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cartes de statistiques
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarteStat(
                titre = "Loyers ce mois",
                valeur = resume.revenusMois,
                modifier = Modifier.weight(1f)
            )
            CarteStat(
                titre = "En retard",
                valeur = "${resume.nombreRetards} locataire(s)",
                couleurValeur = if (resume.nombreRetards > 0) RougeAlerte else VertSucces,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAjouterBien,
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Ajouter un bien")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Mes biens", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(resume.biens) { bien ->
                LigneBien(bien = bien, onClick = { onOuvrirBien(bien) })
            }
        }
    }
}

@Composable
private fun CarteStat(
    titre: String,
    valeur: String,
    couleurValeur: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(titre, fontSize = 11.sp, color = Color.Gray)
            Text(
                valeur,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = couleurValeur
            )
        }
    }
}

@Composable
private fun LigneBien(bien: BienBailleur, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(bien.nom, fontSize = 13.sp)
            val (texte, couleurFond, couleurTexte) = when (bien.statutPaiement) {
                StatutPaiement.PAYE -> Triple("Payé", Color(0xFFE8F5E9), VertSucces)
                StatutPaiement.RETARD -> Triple("Retard", Color(0xFFFFEBEE), RougeAlerte)
            }
            Text(
                texte,
                fontSize = 11.sp,
                color = couleurTexte,
                modifier = Modifier
                    .background(couleurFond, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}