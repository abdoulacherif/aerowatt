package com.locafric.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarreSuperieure(titre: String, onClicMenu: () -> Unit) {
    TopAppBar(
        title = { Text(titre) },
        navigationIcon = {
            IconButton(onClick = onClicMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BleuPrincipal,
            titleContentColor = androidx.compose.ui.graphics.Color.White,
            navigationIconContentColor = androidx.compose.ui.graphics.Color.White
        )
    )
}