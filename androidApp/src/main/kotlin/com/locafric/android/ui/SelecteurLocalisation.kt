package com.locafric.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locafric.android.reseau.DonneesPays
import com.locafric.android.reseau.RetrofitClient

data class Localisation(val pays: String, val ville: String, val quartier: String)

@Composable
fun SelecteurLocalisation(
    localisation: Localisation,
    onLocalisationChange: (Localisation) -> Unit
) {
    var donnees by remember { mutableStateOf<Map<String, DonneesPays>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            val reponse = RetrofitClient.api.recupererLocalisations()
            if (reponse.isSuccessful) {
                donnees = reponse.body() ?: emptyMap()
            }
        } catch (e: Exception) { }
    }

    val paysAvecDrapeaux = donnees.entries.map { "${it.value.drapeau} ${it.key}" }
    val paysActuelAffiche = donnees[localisation.pays]?.let { "${it.drapeau} ${localisation.pays}" } ?: ""
    val villes = donnees[localisation.pays]?.villes?.keys?.toList() ?: emptyList()
    val quartiers = donnees[localisation.pays]?.villes?.get(localisation.ville) ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuDeroulant(
            label = "Pays",
            options = paysAvecDrapeaux,
            valeurSelectionnee = paysActuelAffiche,
            onSelection = { choix ->
                // Retire le drapeau (emoji + espace) pour ne garder que le nom du pays
                val nomPays = choix.substringAfter(" ")
                onLocalisationChange(Localisation(nomPays, "", ""))
            }
        )

        MenuDeroulant(
            label = "Ville",
            options = villes,
            valeurSelectionnee = localisation.ville,
            active = localisation.pays.isNotEmpty(),
            onSelection = { nouvelleVille ->
                onLocalisationChange(localisation.copy(ville = nouvelleVille, quartier = ""))
            }
        )

        MenuDeroulant(
            label = "Quartier",
            options = quartiers,
            valeurSelectionnee = localisation.quartier,
            active = localisation.ville.isNotEmpty(),
            onSelection = { nouveauQuartier ->
                onLocalisationChange(localisation.copy(quartier = nouveauQuartier))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuDeroulant(
    label: String,
    options: List<String>,
    valeurSelectionnee: String,
    active: Boolean = true,
    onSelection: (String) -> Unit
) {
    var ouvert by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = ouvert && active,
        onExpandedChange = { if (active) ouvert = it }
    ) {
        OutlinedTextField(
            value = valeurSelectionnee,
            onValueChange = {},
            readOnly = true,
            enabled = active,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ouvert) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = ouvert && active, onDismissRequest = { ouvert = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelection(option)
                        ouvert = false
                    }
                )
            }
        }
    }
}