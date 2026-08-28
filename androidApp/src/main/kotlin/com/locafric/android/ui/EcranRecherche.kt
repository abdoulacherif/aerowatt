package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BienRecherche(
    val id: String,
    val titre: String,
    val pays: String,
    val ville: String,
    val quartier: String,
    val type: String,
    val capacite: Int,
    val loyer: String
)

@Composable
fun EcranRecherche(
    resultats: List<BienRecherche>,
    onRecherche: (pays: String, ville: String, quartier: String) -> Unit,
    onOuvrirBien: (BienRecherche) -> Unit
) {
    var localisation by remember { mutableStateOf(Localisation("", "", "")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .padding(16.dp)
    ) {
        Text(
            text = "Trouver un logement",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        SelecteurLocalisation(
            localisation = localisation,
            onLocalisationChange = { localisation = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onRecherche(localisation.pays, localisation.ville, localisation.quartier) },
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rechercher")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(resultats) { bien ->
                CarteBien(bien = bien, onClick = { onOuvrirBien(bien) })
            }
        }
    }
}

@Composable
private fun CarteBien(bien: BienRecherche, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(bien.titre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${bien.ville}, ${bien.quartier} — ${bien.pays}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                "${bien.type} — ${bien.capacite} personnes",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                bien.loyer,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OrangeAccent
            )
        }
    }
}