package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Représente les infos affichées à l'écran (viendront du backend plus tard,
// donc rien n'est codé en dur dans le vrai fonctionnement — ceci est un exemple)
data class InfosLocation(
    val nomBien: String,
    val ville: String,
    val quartier: String,
    val nombrePersonnes: Int,
    val montantLoyer: String,
    val dateProchainPaiement: String
)

@Composable
fun EcranAccueilLocataire(
    infos: InfosLocation,
    onVoirContrat: () -> Unit,
    onSignalerReparation: () -> Unit,
    onContacterBailleur: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .padding(16.dp)
    ) {
        // Titre du bien loué
        Text(
            text = infos.nomBien,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${infos.ville}, ${infos.quartier} — ${infos.nombrePersonnes} personnes",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bloc "prochain paiement" avec dégradé rouge -> orange
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(DegradeAccent))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Prochain paiement",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = "${infos.montantLoyer} — ${infos.dateProchainPaiement}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons d'action rapide
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onVoirContrat,
                colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
                modifier = Modifier.weight(1f)
            ) {
                Text("Contrat")
            }
            Button(
                onClick = onSignalerReparation,
                colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
                modifier = Modifier.weight(1f)
            ) {
                Text("Réparation")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contact avec le bailleur
        OutlinedButton(
            onClick = onContacterBailleur,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Contacter le bailleur")
        }
    }
}