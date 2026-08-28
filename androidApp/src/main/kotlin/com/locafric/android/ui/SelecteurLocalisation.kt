package com.locafric.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Données d'exemple — à terme, cette structure viendra du backend
// (pays -> villes -> quartiers), mais le composant fonctionne pareil
val donneesLocalisation: Map<String, Map<String, List<String>>> = mapOf(
    "Sénégal" to mapOf(
        "Dakar" to listOf("Ngor", "Mermoz", "Sacré-Cœur", "Plateau", "Yoff"),
        "Thiès" to listOf("Randoulène", "Grand Standing", "Cité Lamy")
    ),
    "Côte d'Ivoire" to mapOf(
        "Abidjan" to listOf("Cocody", "Marcory", "Yopougon", "Plateau"),
        "Bouaké" to listOf("Air France", "Koko")
    ),
    "Mali" to mapOf(
        "Bamako" to listOf("Hamdallaye", "Badalabougou", "ACI 2000")
    )
)

data class Localisation(val pays: String, val ville: String, val quartier: String)

@Composable
fun SelecteurLocalisation(
    localisation: Localisation,
    onLocalisationChange: (Localisation) -> Unit
) {
    val pays = donneesLocalisation.keys.toList()
    val villes = donneesLocalisation[localisation.pays]?.keys?.toList() ?: emptyList()
    val quartiers = donneesLocalisation[localisation.pays]?.get(localisation.ville) ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuDeroulant(
            label = "Pays",
            options = pays,
            valeurSelectionnee = localisation.pays,
            onSelection = { nouveauPays ->
                // Changer de pays réinitialise ville et quartier
                onLocalisationChange(Localisation(nouveauPays, "", ""))
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