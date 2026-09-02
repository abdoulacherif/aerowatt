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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locafric.android.reseau.*
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

// Bannière d'erreur réutilisable, affichée en haut de n'importe quel écran.
// Elle reste visible tant que l'utilisateur ne l'a pas fermée ou qu'une nouvelle action ne l'a pas remplacée.
@Composable
fun BanniereErreur(message: String?, onFermer: () -> Unit) {
    if (message != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFDECEA))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message, color = RougeAlerte, modifier = Modifier.weight(1f))
            IconButton(onClick = onFermer, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = RougeAlerte)
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
    var contactActuelId by remember { mutableStateOf<Int?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = ecranActuel != Ecran.Connexion) {
        when {
            ecranActuel != Ecran.Principal -> ecranActuel = Ecran.Principal
        }
    }

    // Chargement initial des biens au démarrage de l'app.
    // Toute erreur (réseau, serveur) est affichée dans la bannière au lieu d'être silencieuse.
    LaunchedEffect(Unit) {
        try {
            val reponse = RetrofitClient.api.rechercherBiens(null, null, null)
            if (reponse.isSuccessful) {
                resultatsRecherche = reponse.body()?.map {
                    BienRecherche(it.id.toString(), it.bailleur_id, it.titre, it.pays, it.ville, it.quartier, it.type, it.capacite, "${it.loyer} F")
                } ?: emptyList()
            } else {
                messageErreur = "Chargement initial impossible (code ${reponse.code()})."
            }
        } catch (e: Exception) {
            messageErreur = "Connexion au serveur impossible au démarrage : ${e.message}"
        }
    }

    AnimatedContent(targetState = ecranActuel, label = "navigation_principale", transitionSpec = {
        (fadeIn(tween(220)) togetherWith fadeOut(tween(150)))
    }) { ecran ->
        when (ecran) {
            is Ecran.Connexion -> {
                Column {
                    BanniereErreur(messageErreur) { messageErreur = null }
                    if (chargementEnCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                                        // Erreur renvoyée par notre serveur (ex: 401 mauvais mot de passe)
                                        messageErreur = "Connexion refusée (code ${reponse.code()}) : email ou mot de passe incorrect."
                                    }
                                } catch (e: Exception) {
                                    // Erreur réseau : pas de connexion internet, serveur injoignable, etc.
                                    messageErreur = "Impossible de contacter le serveur : ${e.message}"
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
                                        messageErreur = "Inscription refusée (code ${reponse.code()}) : cet email est peut-être déjà utilisé."
                                    }
                                } catch (e: Exception) {
                                    messageErreur = "Impossible de contacter le serveur : ${e.message}"
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
                        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                            BanniereErreur(messageErreur) { messageErreur = null }
                            if (chargementEnCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

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
                                                    val trouve = resultatsRecherche.find { it.id == bien.id }
                                                    bienSelectionne = trouve
                                                    contactActuelId = trouve?.bailleurId
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
                                                    chargementEnCours = true
                                                    messageErreur = null
                                                    try {
                                                        val reponse = RetrofitClient.api.rechercherBiens(
                                                            pays.ifBlank { null }, ville.ifBlank { null }, quartier.ifBlank { null }
                                                        )
                                                        if (reponse.isSuccessful) {
                                                            resultatsRecherche = reponse.body()?.map {
                                                                BienRecherche(it.id.toString(), it.bailleur_id, it.titre, it.pays, it.ville, it.quartier, it.type, it.capacite, "${it.loyer} F")
                                                            } ?: emptyList()
                                                            if (resultatsRecherche.isEmpty()) {
                                                                messageErreur = "Aucun bien trouvé pour cette recherche."
                                                            }
                                                        } else {
                                                            messageErreur = "Erreur lors de la recherche (code ${reponse.code()})."
                                                        }
                                                    } catch (e: Exception) {
                                                        messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                                    } finally {
                                                        chargementEnCours = false
                                                    }
                                                }
                                            },
                                            onOuvrirBien = { bien ->
                                                bienSelectionne = bien
                                                contactActuelId = bien.bailleurId
                                                ecranActuel = Ecran.DetailBien
                                            }
                                        )
                                    }

                                    OngletPrincipal.MESSAGES -> {
                                        var messagesReels by remember { mutableStateOf<List<Message>>(emptyList()) }

                                        // Recharge le fil de discussion à chaque fois que le contact change
                                        LaunchedEffect(contactActuelId) {
                                            val id = contactActuelId ?: return@LaunchedEffect
                                            try {
                                                val reponse = RetrofitClient.api.recupererFilMessages("Bearer $tokenConnexion", id)
                                                if (reponse.isSuccessful) {
                                                    messagesReels = reponse.body()?.map {
                                                        Message(it.contenu, it.expediteur_id != id)
                                                    } ?: emptyList()
                                                } else {
                                                    messageErreur = "Impossible de charger les messages (code ${reponse.code()})."
                                                }
                                            } catch (e: Exception) {
                                                messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                            }
                                        }

                                        if (contactActuelId == null) {
                                            Column(modifier = Modifier.padding(24.dp)) {
                                                Text("Ouvre un bien puis clique sur \"Contacter le bailleur\" pour démarrer une conversation.")
                                            }
                                        } else {
                                            EcranMessages(
                                                conversations = listOf(Conversation(contactActuelId.toString(), "Bailleur", "")),
                                                conversationOuverte = Conversation(contactActuelId.toString(), "Bailleur", ""),
                                                messages = messagesReels,
                                                onOuvrirConversation = { },
                                                onRetour = { },
                                                onEnvoyerMessage = { texte ->
                                                    scopeCorutine.launch {
                                                        try {
                                                            val reponse = RetrofitClient.api.envoyerMessageApi(
                                                                "Bearer $tokenConnexion",
                                                                RequeteMessage(contactActuelId!!, texte)
                                                            )
                                                            if (reponse.isSuccessful) {
                                                                messagesReels = messagesReels + Message(texte, true)
                                                            } else {
                                                                messageErreur = "Message non envoyé (code ${reponse.code()})."
                                                            }
                                                        } catch (e: Exception) {
                                                            messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    OngletPrincipal.PROFIL -> {
                                        EcranProfil(
                                            profil = ProfilUtilisateur(
                                                nom = nomUtilisateur,
                                                email = "$nomUtilisateur@exemple.com",
                                                role = if (roleConnecte == RoleUtilisateur.BAILLEUR) "Bailleur" else "Locataire"
                                            ),
                                            onDeconnexion = { ecranActuel = Ecran.Connexion },
                                            onDevenirBailleur = {
                                                roleConnecte = RoleUtilisateur.BAILLEUR
                                                ongletActif = OngletPrincipal.ACCUEIL
                                            }
      )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is Ecran.DetailBien -> {
                Column {
                    BanniereErreur(messageErreur) { messageErreur = null }
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
            }

            is Ecran.Contrat -> {
                Column {
                    BanniereErreur(messageErreur) { messageErreur = null }
                    EcranContrat(
                        texteContrat = "CONTRAT DE BAIL\n\nEntre le bailleur et le locataire, il est convenu ce qui suit...\n\n(texte du contrat à personnaliser)",
                        onContratSigne = { ecranActuel = Ecran.Principal }
                    )
                }
            }

            is Ecran.AjouterBien -> {
                Column {
                    BanniereErreur(messageErreur) { messageErreur = null }
                    if (chargementEnCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    EcranAjouterBien(
                        onAnnuler = { ecranActuel = Ecran.Principal },
                        onValider = { nouveauBien ->
                            scopeCorutine.launch {
                                chargementEnCours = true
                                messageErreur = null

                                // Validation de base avant d'envoyer au serveur, pour éviter un aller-retour inutile
                                if (nouveauBien.titre.isBlank() || nouveauBien.localisation.pays.isBlank() ||
                                    nouveauBien.localisation.ville.isBlank() || nouveauBien.type.isBlank() || nouveauBien.loyer.isBlank()) {
                                    messageErreur = "Merci de remplir au moins le titre, le pays, la ville, le type et le loyer."
                                    chargementEnCours = false
                                    return@launch
                                }

                                try {
                                    fun texte(valeur: String) = valeur.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val partiesPhotos = nouveauBien.photos.mapIndexedNotNull { index, uri ->
                                        try {
                                            val fluxEntree = contexte.contentResolver.openInputStream(uri) ?: return@mapIndexedNotNull null
                                            val fichierTemp = File(contexte.cacheDir, "photo_$index.jpg")
                                            fichierTemp.outputStream().use { fluxEntree.copyTo(it) }
                                            val corpsFichier = fichierTemp.asRequestBody("image/*".toMediaTypeOrNull())
                                            MultipartBody.Part.createFormData("photos", fichierTemp.name, corpsFichier)
                                        } catch (e: Exception) {
                                            // Une photo illisible ne doit pas bloquer tout l'envoi : on la saute simplement
                                            null
                                        }
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
                                        ongletActif = OngletPrincipal.ACCUEIL
                                    } else {
                                        // Le serveur a refusé : on affiche le code pour comprendre pourquoi
                                        // (401 = pas connecté, 403 = pas bailleur, 500 = souci serveur/base de données)
                                        messageErreur = "Le bien n'a pas pu être publié (code ${reponse.code()})."
                                    }
                                } catch (e: Exception) {
                                    messageErreur = "Impossible de contacter le serveur : ${e.message}"
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
}            