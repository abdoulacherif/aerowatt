package com.locafric.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Liste des 54 pays d'Afrique avec leurs principales villes.
// Structure temporaire — à terme, ces données viendront du backend.
val donneesLocalisation: Map<String, Map<String, List<String>>> = mapOf(
    "Afrique du Sud" to mapOf("Johannesburg" to listOf("Sandton", "Soweto"), "Le Cap" to listOf("Centre-ville", "Bo-Kaap"), "Durban" to listOf("Umhlanga")),
    "Algérie" to mapOf("Alger" to listOf("Hydra", "Bab Ezzouar"), "Oran" to listOf("Centre-ville"), "Constantine" to listOf("Centre-ville")),
    "Angola" to mapOf("Luanda" to listOf("Ingombota", "Talatona")),
    "Bénin" to mapOf("Cotonou" to listOf("Cadjehoun", "Akpakpa"), "Porto-Novo" to listOf("Centre-ville")),
    "Botswana" to mapOf("Gaborone" to listOf("Centre-ville")),
    "Burkina Faso" to mapOf("Ouagadougou" to listOf("Ouaga 2000", "Zone du Bois"), "Bobo-Dioulasso" to listOf("Centre-ville")),
    "Burundi" to mapOf("Bujumbura" to listOf("Rohero", "Kiriri")),
    "Cabo Verde" to mapOf("Praia" to listOf("Plateau")),
    "Cameroun" to mapOf("Douala" to listOf("Bonanjo", "Akwa", "Bonapriso"), "Yaoundé" to listOf("Bastos", "Omnisport")),
    "République centrafricaine" to mapOf("Bangui" to listOf("Centre-ville")),
    "Comores" to mapOf("Moroni" to listOf("Centre-ville")),
    "Congo-Brazzaville" to mapOf("Brazzaville" to listOf("Bacongo", "Poto-Poto")),
    "Congo-Kinshasa (RDC)" to mapOf("Kinshasa" to listOf("Gombe", "Limete", "Ngaliema"), "Lubumbashi" to listOf("Centre-ville")),
    "Côte d'Ivoire" to mapOf("Abidjan" to listOf("Cocody", "Marcory", "Yopougon", "Plateau"), "Bouaké" to listOf("Air France", "Koko")),
    "Djibouti" to mapOf("Djibouti (ville)" to listOf("Centre-ville", "Balbala")),
    "Égypte" to mapOf("Le Caire" to listOf("Zamalek", "Maadi", "Nasr City"), "Alexandrie" to listOf("Centre-ville")),
    "Érythrée" to mapOf("Asmara" to listOf("Centre-ville")),
    "Eswatini" to mapOf("Mbabane" to listOf("Centre-ville")),
    "Éthiopie" to mapOf("Addis-Abeba" to listOf("Bole", "Kazanchis")),
    "Gabon" to mapOf("Libreville" to listOf("Glass", "Batterie IV", "Nombakélé")),
    "Gambie" to mapOf("Banjul" to listOf("Centre-ville")),
    "Ghana" to mapOf("Accra" to listOf("East Legon", "Osu", "Airport Residential"), "Kumasi" to listOf("Centre-ville")),
    "Guinée" to mapOf("Conakry" to listOf("Kaloum", "Ratoma", "Dixinn")),
    "Guinée-Bissau" to mapOf("Bissau" to listOf("Centre-ville")),
    "Guinée équatoriale" to mapOf("Malabo" to listOf("Centre-ville")),
    "Kenya" to mapOf("Nairobi" to listOf("Westlands", "Karen", "Kilimani"), "Mombasa" to listOf("Nyali")),
    "Lesotho" to mapOf("Maseru" to listOf("Centre-ville")),
    "Liberia" to mapOf("Monrovia" to listOf("Sinkor", "Mamba Point")),
    "Libye" to mapOf("Tripoli" to listOf("Centre-ville"), "Benghazi" to listOf("Centre-ville")),
    "Madagascar" to mapOf("Antananarivo" to listOf("Analakely", "Ivandry")),
    "Malawi" to mapOf("Lilongwe" to listOf("Area 47"), "Blantyre" to listOf("Centre-ville")),
    "Mali" to mapOf("Bamako" to listOf("Hamdallaye", "Badalabougou", "ACI 2000")),
    "Maroc" to mapOf("Casablanca" to listOf("Maarif", "Gauthier", "Ain Diab"), "Rabat" to listOf("Agdal", "Hay Riad"), "Marrakech" to listOf("Guéliz")),
    "Maurice" to mapOf("Port-Louis" to listOf("Centre-ville")),
    "Mauritanie" to mapOf("Nouakchott" to listOf("Tevragh-Zeina", "Ksar")),
    "Mozambique" to mapOf("Maputo" to listOf("Sommerschield", "Polana")),
    "Namibie" to mapOf("Windhoek" to listOf("Klein Windhoek")),
    "Niger" to mapOf("Niamey" to listOf("Plateau", "Yantala")),
    "Nigeria" to mapOf("Lagos" to listOf("Victoria Island", "Lekki", "Ikeja"), "Abuja" to listOf("Maitama", "Wuse")),
    "Ouganda" to mapOf("Kampala" to listOf("Kololo", "Nakasero")),
    "Rwanda" to mapOf("Kigali" to listOf("Kacyiru", "Kimihurura", "Nyarutarama")),
    "Sao Tomé-et-Principe" to mapOf("São Tomé" to listOf("Centre-ville")),
    "Sénégal" to mapOf("Dakar" to listOf("Ngor", "Mermoz", "Sacré-Cœur", "Plateau", "Yoff"), "Thiès" to listOf("Randoulène", "Grand Standing")),
    "Seychelles" to mapOf("Victoria" to listOf("Centre-ville")),
    "Sierra Leone" to mapOf("Freetown" to listOf("Aberdeen", "Hill Station")),
    "Somalie" to mapOf("Mogadiscio" to listOf("Centre-ville")),
    "Soudan" to mapOf("Khartoum" to listOf("Centre-ville")),
    "Soudan du Sud" to mapOf("Juba" to listOf("Centre-ville")),
    "Tanzanie" to mapOf("Dar es Salaam" to listOf("Masaki", "Oyster Bay"), "Dodoma" to listOf("Centre-ville")),
    "Tchad" to mapOf("N'Djamena" to listOf("Centre-ville")),
    "Togo" to mapOf("Lomé" to listOf("Tokoin", "Agbalépédogan")),
    "Tunisie" to mapOf("Tunis" to listOf("La Marsa", "Lac 1", "Menzah"), "Sfax" to listOf("Centre-ville")),
    "Zambie" to mapOf("Lusaka" to listOf("Kabulonga", "Woodlands")),
    "Zimbabwe" to mapOf("Harare" to listOf("Borrowdale", "Avondale"))
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