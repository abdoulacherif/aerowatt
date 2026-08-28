package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContenuMenuBurger(
    nomUtilisateur: String,
    onFermer: () -> Unit
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight().background(FondClair).padding(16.dp)) {
            Text("LOCAFRIC", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BleuPrincipal)
            Text(nomUtilisateur, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))

            listOf("Mes biens", "Mes contrats", "Paiements", "Notifications", "Aide", "Paramètres").forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item) },
                    selected = false,
                    onClick = onFermer,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}