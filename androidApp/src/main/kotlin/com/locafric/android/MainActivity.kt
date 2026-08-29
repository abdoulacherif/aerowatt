package com.locafric.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locafric.android.reseau.RequeteConnexion
import com.locafric.android.reseau.RequeteInscription
import com.locafric.android.reseau.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.locafric.android.ui.*
import java.io.File

sealed class Ecran {
    object Connexion : Ecran()
    object Principal : Ecran()
    object DetailBien : Ecran()
    object Contrat : Ecran()
    object AjouterBien : Ecran()
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
    val contexte = LocalContext.current
    val scopeCorutine = rememberCoroutineScope()

    var ecranActuel by remember { mutableStateOf<Ecran>(Ecran.Connexion) }
    var ongletActif by remember { mutableStateOf(OngletPrincipal.ACCUEIL) }
    var roleConnecte by remember { mutableStateOf(RoleUtilisateur.LOCATAIRE) }
    var nomUtilisateur by remember { mutableStateOf("Utilisateur") }
    var tokenConnexion by remember { mutableStateOf("") }
    var messageErreur by remember { mutableStateOf<String?>(null) }
    var chargementEnCours by remember { mutableStateOf(false) }

    var resultatsRecherche by remember { mutableStateOf<List<BienRecherche>>(emptyList()) }
    var bienSelectionne by remember { mutableStateOf<BienRecherche?>(null) }

    var conversationOuverte by remember { mutableStateOf<Conversation?>(null) }
    val messagesExemple = remember { mutableStateListOf(Message("Bonjour, le loyer est bien reçu.", false)) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = ecranActuel != Ecran.Connexion) {
        when {
            conversationOuverte != null -> conversationOuverte = null
            ecranActuel != Ecran.Principal -> ecranActuel = Ecran.Principal
        }
    }

    LaunchedEffect(Unit) {
        try {
            val reponse = RetrofitClient.api.rechercherBiens(null, null, null)
            if (reponse.isSuccessful) {
                resultatsRecherche = reponse.body()?.map {
                    BienRecherche(it.id.toString(), it.titre, it.pays, it.ville, it.quartier, it.type, it.capacite, "${it.loyer} F")
                } ?: emptyList()
            }
        } catch (e: Exception) {
            // Pas de connexion ou serveur indisponible — la recherche restera vide pour l'instant
        }
    }

    AnimatedContent(targetState = ecranActuel, label = "navigation_principale", transitionSpec = {
        (fadeIn(tween(220)) togetherWith fadeOut(tween(150)))
    }) { ecran ->
        when (ecran) {
            is Ecran.Connexion -> {
                Column {
                    if (chargementEnCours) LinearProgressIndicator(modifier = Modifier.fillMaxSize())
                    messageErreur?.let {
                        Text(it, color = RougeAlerte, modifier = Modifier.padding(8.dp))
                    }
                    EcranConnexion(
                        onConnexion = { infos ->
                            scopeCorutine.launch {
                                chargementEnCours = true
                                messageErreur = null
                                try {
                                    val reponse = RetrofitClient.api.connexion(RequeteConnexion(infos.email, infos.motDePasse))
                                    if (reponse.isSuccessful) {
                                        val corps = reponse.body()!!
                                        tokenConnexion = corps.token
                                        roleConnecte = if (corps.utilisateur.role == "bailleur") RoleUtilisateur.BAILLEUR else RoleUtilisateur.LOCATAIRE
                                        nomUtilisateur = corps.utilisateur.nomComplet ?: corps.utilisateur.email
                                        ecranActuel = Ecran.Principal
                                    } else {
                                        messageErreur = "Email ou mot de passe incorrect."
                                    }
                                } catch (e: Exception) {
                                    messageErreur = "Impossible de contacter le serveur."
                                } finally {
                                    chargementEnCours = false
                                }
                            }
                        },
                        onInscription = { infos ->
                            scopeCorutine.launch {
                                chargementEnCours = true
                                messageErreur = null
                                try {
                                    val roleTexte = if (infos.role == RoleUtilisateur.BAILLEUR) "bailleur" else "locataire"
                                    val reponse = RetrofitClient.api.inscription(
                                        RequeteInscription(infos.nomComplet, infos.email, infos.motDePasse, roleTexte)
                                    )
                                    if (reponse.isSuccessful) {
                                        val corps = reponse.body()!!
                                        tokenConnexion = corps.token
                                        roleConnecte = infos.role
                                        nomUtilisateur = infos.nomComplet
                                        ecranActuel = Ecran.Principal
                                    } else {
                                        messageErreur = "Cet email est peut-être déjà utilisé."
                                    }
                                } catch (e: Exception) {
                                    messageErreur = "Impossible de contacter le serveur."
                                } finally {
                                    chargementEnCours = false
                                }
                            }
                        }
                    )
                }
            }

            is Ecran.Principal -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ContenuMenuBurger(
                            nomUtilisateur = nomUtilisateur,
                            onFermer = { scopeCorutine.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            BarreSuperieure(
                                titre = "LOCAFRIC",
                                onClicMenu = { scopeCorutine.launch { drawerState.open() } }
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
                                                    nombreBiens = resultatsRecherche.size,
                                                    revenusMois = "—",
                                                    nombreRetards = 0,
                                                    biens = resultatsRecherche.map { BienBailleur(it.id, it.titre, StatutPaiement.PAYE) }
                                                ),
                                                onOuvrirBien = { bien ->
                                                    bienSelectionne = resultatsRecherche.find { it.id == bien.id }
                                                    ecranActuel = Ecran.DetailBien
                                                },
                                                onAjouterBien = { ecranActuel = Ecran.AjouterBien }
                                            )
                                        } else {
                                            EcranAccueilLocataire(
                                                infos = InfosLocation(
                                                    nomBien = "Aucune location active",
                                                    ville = "",
                                                    quartier = "",
                                                    nombrePersonnes = 0,
                                                    montantLoyer = "—",
                                                    dateProchainPaiement = "—"
                                                ),
                                                onVoirContrat = { ecranActuel = Ecran.Contrat },
                                                onSignalerReparation = { },
                                                onContacterBailleur = { ongletActif = OngletPrincipal.MESSAGES }
                                            )
                                        }
                                    }

                                    OngletPrincipal.RECHERCHE -> {
                                        EcranRecherche(
                                            resultats = resultatsRecherche,
                                            onRecherche = { pays, ville, quartier ->
                                                scopeCorutine.launch {
                                                    try {
                                                        val reponse = RetrofitClient.api.rechercherBiens(
                                                            pays.ifBlank { null }, ville.ifBlank { null }, quartier.ifBlank { null }
                                                        )
                                                        if (reponse.isSuccessful) {
                                                            resultatsRecherche = reponse.body()?.map {
                                                                BienRecherche(it.id.toString(), it.titre, it.pays, it.ville, it.quartier, it.type, it.capacite, "${it.loyer} F")
                                                            } ?: emptyList()
                                                        }
                                                    } catch (e: Exception) { }
                                                }
                                            },
                                            onOuvrirBien = { bien ->
                                                bienSelectionne = bien
                                                ecranActuel = Ecran.DetailBien
                                            }
                                        )
                                    }

                                    OngletPrincipal.MESSAGES -> {
                                        EcranMessages(
                                            conversations = listOf(Conversation("1", "Amadou (bailleur)", "Le loyer est bien reçu.")),
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
                val bien = bienSelectionne
                EcranDetailBien(
                    bien = DetailBien(
                        titre = bien?.titre ?: "",
                        pays = bien?.pays ?: "",
                        ville = bien?.ville ?: "",
                        quartier = bien?.quartier ?: "",
                        type = bien?.type ?: "",
                        capacite = bien?.capacite ?: 0,
                        loyer = bien?.loyer ?: "",
                        description = "Détails complets à venir."
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

            is Ecran.AjouterBien -> {
                EcranAjouterBien(
                    onAnnuler = { ecranActuel = Ecran.Principal },
                    onValider = { nouveauBien ->
                        scopeCorutine.launch {
                            chargementEnCours = true
                            try {
                                fun texte(valeur: String) = valeur.toRequestBody("text/plain".toMediaTypeOrNull())
                                val partiesPhotos = nouveauBien.photos.mapIndexedNotNull { index, uri ->
                                    val fluxEntree = contexte.contentResolver.openInputStream(uri) ?: return@mapIndexedNotNull null
                                    val fichierTemp = File(contexte.cacheDir, "photo_$index.jpg")
                                    fichierTemp.outputStream().use { fluxEntree.copyTo(it) }
                                    val corpsFichier = fichierTemp.asRequestBody("image/*".toMediaTypeOrNull())
                                    MultipartBody.Part.createFormData("photos", fichierTemp.name, corpsFichier)
                                }

                                val reponse = RetrofitClient.api.ajouterBien(
                                    token = "Bearer $tokenConnexion",
                                    titre = texte(nouveauBien.titre),
                                    pays = texte(nouveauBien.localisation.pays),
                                    ville = texte(nouveauBien.localisation.ville),
                                    quartier = texte(nouveauBien.localisation.quartier),
                                    type = texte(nouveauBien.type),
                                    capacite = texte(nouveauBien.capacite.toString()),
                                    loyer = texte(nouveauBien.loyer),
                                    description = texte(nouveauBien.description),
                                    photos = partiesPhotos
                                )
                                if (reponse.isSuccessful) {
                                    ecranActuel = Ecran.Principal
                                } else {
                                    messageErreur = "Erreur lors de l'ajout du bien."
                                }
                            } catch (e: Exception) {
                                messageErreur = "Impossible de contacter le serveur."
                            } finally {
                                chargementEnCours = false
                            }
                        }
                    }
                )
            }
        }
    }
}