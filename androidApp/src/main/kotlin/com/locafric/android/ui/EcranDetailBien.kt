package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DetailBien(
    val titre: String,
    val pays: String,
    val ville: String,
    val quartier: String,
    val type: String,
    val capacite: Int,
    val loyer: String,
    val description: String
)

@Composable
fun EcranDetailBien(
    bien: DetailBien,
    onEnvoyerDemande: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .padding(16.dp)
    ) {
        // Zone image (placeholder en attendant la vraie photo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFDDE3EA))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(bien.titre, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text(
            "${bien.ville}, ${bien.quartier} — ${bien.pays}",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiquette(texte = bien.type)
            Etiquette(texte = "${bien.capacite} personnes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            bien.loyer,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeAccent
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(bien.description, fontSize = 13.sp, color = Color.DarkGray)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onEnvoyerDemande,
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Envoyer une demande")
        }
    }
}

@Composable
private fun Etiquette(texte: String) {
    Text(
        texte,
        fontSize = 12.sp,
        color = BleuPrincipal,
        modifier = Modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}