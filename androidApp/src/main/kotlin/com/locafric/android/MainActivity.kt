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
import androidx.compose.foundation.background
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

@Composable
fun BanniereMessage(message: String?, couleur: Color, onFermer: () -> Unit) {
    if (message != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(couleur.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message, color = couleur, modifier = Modifier.weight(1f))
            IconButton(onClick = onFermer, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = couleur)
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
    var messageSucces by remember { mutableStateOf<String?>(null) }
    var chargementEnCours by remember { mutableStateOf(false) }

    var resultatsRecherche by remember { mutableStateOf<List<BienRecherche>>(emptyList()) }
    var mesBiensBailleur by remember { mutableStateOf<List<BienRecherche>>(emptyList()) }

    var bienSelectionne by remember { mutableStateOf<DetailBienReponse?>(null) }
    var contactActuelId by remember { mutableStateOf<Int?>(null) }
    var chargementDetailBien by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = ecranActuel != Ecran.Connexion) {
        when {
            ecranActuel != Ecran.Principal -> ecranActuel = Ecran.Principal
        }
    }

    fun convertirBien(it: BienReponse) = BienRecherche(
        it.id.toString(), it.bailleur_id, it.titre, it.pays, it.ville, it.quartier, it.type, it.capacite, "${it.loyer} F"
    )

    suspend fun chargerBiensPublics() {
        try {
            val reponse = RetrofitClient.api.rechercherBiens(null, null, null)
            if (reponse.isSuccessful) {
                resultatsRecherche = reponse.body()?.map { convertirBien(it) } ?: emptyList()
            } else {
                messageErreur = "Chargement impossible (code ${reponse.code()})."
            }
        } catch (e: Exception) {
            messageErreur = "Connexion au serveur impossible : ${e.message}"
        }
    }

    suspend fun chargerMesBiens() {
        try {
            val reponse = RetrofitClient.api.mesBiens("Bearer $tokenConnexion")
            if (reponse.isSuccessful) {
                mesBiensBailleur = reponse.body()?.map { convertirBien(it) } ?: emptyList()
            } else {
                messageErreur = "Impossible de charger vos biens (code ${reponse.code()})."
            }
        } catch (e: Exception) {
            messageErreur = "Connexion au serveur impossible : ${e.message}"
        }
    }

    LaunchedEffect(Unit) {
        chargerBiensPublics()
    }

    LaunchedEffect(roleConnecte, tokenConnexion) {
        if (roleConnecte == RoleUtilisateur.BAILLEUR && tokenConnexion.isNotBlank()) {
            chargerMesBiens()
        }
    }

    AnimatedContent(targetState = ecranActuel, label = "navigation_principale", transitionSpec = {
        (fadeIn(tween(220)) togetherWith fadeOut(tween(150)))
    }) { ecran ->
        when (ecran) {
            is Ecran.Connexion -> {
                Column {
                    BanniereMessage(messageErreur, RougeAlerte) { messageErreur = null }
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
                                        messageErreur = "Connexion refusée (code ${reponse.code()}) : email ou mot de passe incorrect."
                                    }
                                } catch (e: Exception) {
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
                            BanniereMessage(messageErreur, RougeAlerte) { messageErreur = null }
                            BanniereMessage(messageSucces, VertSucces) { messageSucces = null }
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
                                                    nombreBiens = mesBiensBailleur.size,
                                                    revenusMois = "—",
                                                    nombreRetards = 0,
                                                    biens = mesBiensBailleur.map { BienBailleur(it.id, it.titre, StatutPaiement.PAYE) }
                                                ),
                                                onOuvrirBien = { bien ->
                                                    scopeCorutine.launch {
                                                        chargementDetailBien = true
                                                        try {
                                                            val reponse = RetrofitClient.api.obtenirDetailBien(bien.id.toInt())
                                                            if (reponse.isSuccessful) {
                                                                bienSelectionne = reponse.body()
                                                                contactActuelId = reponse.body()?.bailleur_id
                                                                ecranActuel = Ecran.DetailBien
                                                            } else {
                                                                messageErreur = "Impossible de charger le bien (code ${reponse.code()})."
                                                            }
                                                        } catch (e: Exception) {
                                                            messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                                        } finally {
                                                            chargementDetailBien = false
                                                        }
                                                    }
                                                },
                                                onAjouterBien = { ecranActuel = Ecran.AjouterBien }
                                            )
                                        } else {EcranAccueilLocataire(
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
                                                            resultatsRecherche = reponse.body()?.map { convertirBien(it) } ?: emptyList()
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
                                                scopeCorutine.launch {
                                                    chargementDetailBien = true
                                                    try {
                                                        val reponse = RetrofitClient.api.obtenirDetailBien(bien.id.toInt())
                                                        if (reponse.isSuccessful) {
                                                            bienSelectionne = reponse.body()
                                                            contactActuelId = reponse.body()?.bailleur_id
                                                            ecranActuel = Ecran.DetailBien
                                                        } else {
                                                            messageErreur = "Impossible de charger le bien (code ${reponse.code()})."
                                                        }
                                                    } catch (e: Exception) {
                                                        messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                                    } finally {
                                                        chargementDetailBien = false
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    OngletPrincipal.MESSAGES -> {
                                        var messagesReels by remember { mutableStateOf<List<Message>>(emptyList()) }

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
                                                scopeCorutine.launch {
                                                    chargementEnCours = true
                                                    try {
                                                        val reponse = RetrofitClient.api.devenirBailleur("Bearer $tokenConnexion")
                                                        if (reponse.isSuccessful) {
                                                            val corps = reponse.body()!!
                                                            tokenConnexion = corps.token
                                                            roleConnecte = RoleUtilisateur.BAILLEUR
                                                            ongletActif = OngletPrincipal.ACCUEIL
                                                            messageSucces = "Tu es maintenant aussi bailleur."
                                                        } else {
                                                            messageErreur = "Impossible de changer de rôle (code ${reponse.code()})."
                                                        }
                                                    } catch (e: Exception) {
                                                        messageErreur = "Impossible de contacter le serveur : ${e.message}"
                                                    } finally {
                                                        chargementEnCours = false
                                                    }
                                                }
                                            },
                                            onRedevenirLocataire = {
                                                scopeCorutine.launch {
                                                    chargementEnCours = true
                                                    try {
                                                        val reponse = RetrofitClient.api.redevenirLocataire("Bearer $tokenConnexion")
                                                        if (reponse.isSuccessful) {
                                                            val corps = reponse.body()!!
                                                            tokenConnexion = corps.token
                                                            roleConnecte = RoleUtilisateur.LOCATAIRE
                                                            ongletActif = OngletPrincipal.ACCUEIL
                                                            messageSucces = "Tu es de nouveau en mode locataire."
                                                        } else {
                                                            messageErreur = "Impossible de changer de rôle (code ${reponse.code()})."
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
                }
            }

            is Ecran.DetailBien -> {
                Column {
                    BanniereMessage(messageErreur, RougeAlerte) { messageErreur = null }
                    if (chargementDetailBien) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
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
                            description = bien?.description ?: "Aucune description fournie.",
                            photos = bien?.photos ?: emptyList()
                        ),
                        onEnvoyerDemande = { ecranActuel = Ecran.Principal }
                    )
                }
            }

            is Ecran.Contrat -> {
                Column {
                    BanniereMessage(messageErreur, RougeAlerte) { messageErreur = null }
                    EcranContrat(
                        texteContrat = "CONTRAT DE BAIL\n\nEntre le bailleur et le locataire, il est convenu ce qui suit...\n\n(texte du contrat à personnaliser)",
                        onContratSigne = { ecranActuel = Ecran.Principal }
                    )
                }
            }

            is Ecran.AjouterBien -> {
                Column {
                    BanniereMessage(messageErreur, RougeAlerte) { messageErreur = null }
                    if (chargementEnCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    EcranAjouterBien(
                        onAnnuler = { ecranActuel = Ecran.Principal },
                        onValider = { nouveauBien ->
                            scopeCorutine.launch {
                                chargementEnCours = true
                                messageErreur = null

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
                                        messageSucces = "Bien publié avec succès !"
                                        chargerMesBiens()
                                        chargerBiensPublics()
                                        ecranActuel = Ecran.Principal
                                        ongletActif = OngletPrincipal.ACCUEIL
                                    } else {
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