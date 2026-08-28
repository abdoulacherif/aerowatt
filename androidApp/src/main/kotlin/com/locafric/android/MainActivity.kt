package com.locafric.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.locafric.android.ui.*

// Écrans possibles dans l'application
sealed class Ecran {
    object Connexion : Ecran()
    object Principal : Ecran() // contient la barre du bas
    object DetailBien : Ecran()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocafricApp()
                }
            }
        }
    }
}

@Composable
fun LocafricApp() {
    var ecranActuel by remember { mutableStateOf<Ecran>(Ecran.Connexion) }
    var ongletActif by remember { mutableStateOf(OngletPrincipal.ACCUEIL) }
    var roleConnecte by remember { mutableStateOf(RoleUtilisateur.LOCATAIRE) }

    when (ecranActuel) {
        is Ecran.Connexion -> {
            EcranConnexion(
                onConnexion = { infos ->
                    roleConnecte = infos.role
                    ecranActuel = Ecran.Principal
                },
                onInscription = { infos ->
                    roleConnecte = infos.role
                    ecranActuel = Ecran.Principal
                }
            )
        }

        is Ecran.Principal -> {
            Scaffold(
                topBar = {
                    BarreSuperieure(
                        titre = "LOCAFRIC",
                        onClicMenu = { /* menu burger géré plus tard */ }
                    )
                },
                bottomBar = {
                    BarreNavigationBasse(
                        ongletActif = ongletActif,
                        onOngletSelectionne = { ongletActif = it }
                    )
                }
            ) { padding ->
                Surface(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (ongletActif) {
                        OngletPrincipal.ACCUEIL -> {
                            if (roleConnecte == RoleUtilisateur.BAILLEUR) {
                                EcranTableauBordBailleur(
                                    resume = ResumeBailleur(
                                        nomBailleur = "Amadou",
                                        nombreBiens = 3,
                                        revenusMois = "450 000 F",
                                        nombreRetards = 1,
                                        biens = listOf(
                                            BienBailleur("1", "Villa Ngor, chambre salon", StatutPaiement.PAYE),
                                            BienBailleur("2", "Sacré-Cœur, appt 2 ch.", StatutPaiement.RETARD)
                                        )
                                    ),
                                    onOuvrirBien = { ecranActuel = Ecran.DetailBien },
                                    onAjouterBien = { }
                                )
                            } else {
                                EcranAccueilLocataire(
                                    infos = InfosLocation(
                                        nomBien = "Villa Ngor, chambre salon",
                                        ville = "Dakar",
                                        quartier = "Ngor",
                                        nombrePersonnes = 2,
                                        montantLoyer = "75 000 F",
                                        dateProchainPaiement = "5 sept."
                                    ),
                                    onVoirContrat = { },
                                    onSignalerReparation = { },
                                    onContacterBailleur = { }
                                )
                            }
                        }

                        OngletPrincipal.RECHERCHE -> {
                            EcranRecherche(
                                resultats = listOf(
                                    BienRecherche("1", "Studio moderne", "Sénégal", "Dakar", "Mermoz", "Chambre salon", 2, "60 000 F"),
                                    BienRecherche("2", "Appartement 2 chambres", "Sénégal", "Dakar", "Sacré-Cœur", "Appartement", 4, "120 000 F")
                                ),
                                onRecherche = { _, _, _ -> },
                                onOuvrirBien = { ecranActuel = Ecran.DetailBien }
                            )
                        }

                        OngletPrincipal.MESSAGES -> {
                            // Écran de messagerie à développer plus tard
                        }

                        OngletPrincipal.PROFIL -> {
                            // Écran de profil à développer plus tard
                        }
                    }
                }
            }
        }

        is Ecran.DetailBien -> {
            EcranDetailBien(
                bien = DetailBien(
                    titre = "Studio moderne",
                    pays = "Sénégal",
                    ville = "Dakar",
                    quartier = "Mermoz",
                    type = "Chambre salon",
                    capacite = 2,
                    loyer = "60 000 F / mois",
                    description = "Studio meublé, calme, proche des commerces. Eau et électricité incluses."
                ),
                onEnvoyerDemande = { ecranActuel = Ecran.Principal }
            )
        }
    }
}