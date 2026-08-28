package com.locafric.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

enum class OngletPrincipal { ACCUEIL, RECHERCHE, MESSAGES, PROFIL }

@Composable
fun BarreNavigationBasse(
    ongletActif: OngletPrincipal,
    onOngletSelectionne: (OngletPrincipal) -> Unit
) {
    NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
        NavigationBarItem(
            selected = ongletActif == OngletPrincipal.ACCUEIL,
            onClick = { onOngletSelectionne(OngletPrincipal.ACCUEIL) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
            label = { Text("Accueil") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BleuPrincipal, selectedTextColor = BleuPrincipal)
        )
        NavigationBarItem(
            selected = ongletActif == OngletPrincipal.RECHERCHE,
            onClick = { onOngletSelectionne(OngletPrincipal.RECHERCHE) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Recherche") },
            label = { Text("Recherche") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BleuPrincipal, selectedTextColor = BleuPrincipal)
        )
        NavigationBarItem(
            selected = ongletActif == OngletPrincipal.MESSAGES,
            onClick = { onOngletSelectionne(OngletPrincipal.MESSAGES) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Messages") },
            label = { Text("Messages") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BleuPrincipal, selectedTextColor = BleuPrincipal)
        )
        NavigationBarItem(
            selected = ongletActif == OngletPrincipal.PROFIL,
            onClick = { onOngletSelectionne(OngletPrincipal.PROFIL) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BleuPrincipal, selectedTextColor = BleuPrincipal)
        )
    }
}