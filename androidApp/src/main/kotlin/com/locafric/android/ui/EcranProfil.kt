package com.locafric.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProfilUtilisateur(val nom: String, val email: String, val role: String)

@Composable
fun EcranProfil(
    profil: ProfilUtilisateur,
    onDeconnexion: () -> Unit,
    onDevenirBailleur: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().background(FondClair).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(42.dp))
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(DegradeAccent)),
            contentAlignment = Alignment.Center
        ) {
            Text(profil.nom.take(1), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(profil.nom, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(profil.email, fontSize = 12.sp, color = Color.Gray)
        Text(
            profil.role,
            fontSize = 11.sp,
            color = BleuPrincipal,
            modifier = Modifier
                .padding(top = 6.dp)
                .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        LigneProfil(titre = "Mes informations")
        LigneProfil(titre = "Documents et contrats")
        if (profil.role == "Locataire") {
            LigneProfil(titre = "Devenir bailleur (mettre un bien en location)", onClick = onDevenirBailleur)
        }
        LigneProfil(titre = "Paramètres")
        LigneProfil(titre = "Aide et support")

        Spacer(modifier = Modifier.weight(1f))

        BoutonAnime(
            texte = "Se déconnecter",
            couleur = RougeAlerte,
            onClick = onDeconnexion
        )
    }
}

@Composable
private fun LigneProfil(titre: String, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titre, fontSize = 13.sp)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
private fun BoutonAnime(texte: String, couleur: Color, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "scale")
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = couleur),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Text(texte)
    }
}