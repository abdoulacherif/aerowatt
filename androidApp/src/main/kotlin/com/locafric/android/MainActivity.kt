package com.locafric.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.locafric.android.ui.*

sealed class Ecran {
    object Connexion : Ecran()
    object Principal : Ecran()
    object DetailBien : Ecran()
    object Contrat : Ecran()
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
    var nomUtilisateur by remember { mutableStateOf("Utilisateur") }

    var conversationOuverte by remember { mutableStateOf<Conversation?>(null) }
    val messagesExemple = remember { mutableStateListOf(Message("Bonjour, le loyer est bien reçu.", false)) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    AnimatedContent(targetState = ecranActuel, label = "navigation_principale", transitionSpec = {
        (fadeIn(tween(220)) togetherWith fadeOut(tween(150)))
    }) { ecran ->
        when (ecran) {
            is Ecran.Connexion -> {
                EcranConnexion(
                    onConnexion = { infos ->
                        roleConnecte = infos.role
                        nomUtilisateur = infos.email.substringBefore("@").ifBlank { "Utilisateur" }
                        ecranActuel = Ecran.Principal
                    },
                    onInscription = { infos ->
                        roleConnecte = infos.role
                        nomUtilisateur = infos.nomComplet.ifBlank { "Utilisateur" }
                        ecranActuel = Ecran.Principal
                    }
                )
            }

            is Ecran.Principal -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ContenuMenuBurger(
                            nomUtilisateur = nomUtilisateur,
                            onFermer = { scope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            BarreSuperieure(
                                titre = "LOCAFRIC",
                                onClicMenu = { scope.launch { drawerState.open() } }
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
                            AnimatedContent(targetState = ongletActif, label = "onglets", transitionSpec = {
                                (fadeIn(tween(200)) togetherWith fadeOut(tween(120)))
                            }) { onglet ->
                                when (onglet) {
                                    OngletPrincipal.ACCUEIL -> {
                                        if (roleConnecte == RoleUtilisateur.BAILLEUR) {
                                            EcranTableauBordBailleur(
                                                resume = ResumeBailleur(
                                                    nomBailleur = nomUtilisateur,
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
                                                onVoirContrat = { ecranActuel = Ecran.Contrat },
                                                onSignalerReparation = { },
                                                onContacterBailleur = { ongletActif = OngletPrincipal.MESSAGES }
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
                                        EcranMessages(
                                            conversations = listOf(
                                                Conversation("1", "Amadou (bailleur)", "Le loyer est bien reçu.")
                                            ),
                                            conversationOuverte = conversationOuverte,
                                            messages = messagesExemple,
                                            onOuvrirConversation = { conversationOuverte = it },
                                            onRetour = { conversationOuverte = null },
                                            onEnvoyerMessage = { texte -> messagesExemple.add(Message(texte, true)) }
                                        )
                                    }

                                    OngletPrincipal.PROFIL -> {
                                        EcranProfil(
                                            profil = ProfilUtilisateur(
                                                nom = nomUtilisateur,
                                                email = "$nomUtilisateur@exemple.com",
                                                role = if (roleConnecte == RoleUtilisateur.BAILLEUR) "Bailleur" else "Locataire"
                                            ),
                                            onDeconnexion = { ecranActuel = Ecran.Connexion }
                                        )
                                    }
                                }
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

            is Ecran.Contrat -> {
                EcranContrat(
                    texteContrat = "CONTRAT DE BAIL\n\nEntre le bailleur et le locataire, il est convenu ce qui suit...\n\n(texte du contrat à personnaliser)",
                    onContratSigne = { ecranActuel = Ecran.Principal }
                )
            }
        }
    }
}