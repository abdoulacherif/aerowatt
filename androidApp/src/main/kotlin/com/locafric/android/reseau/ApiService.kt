package com.locafric.android.reseau

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

data class RequeteInscription(val nomComplet: String, val email: String, val motDePasse: String, val role: String)
data class RequeteConnexion(val email: String, val motDePasse: String)
data class UtilisateurReponse(val id: Int, val nomComplet: String? = null, val email: String, val role: String)
data class AuthReponse(val utilisateur: UtilisateurReponse, val token: String)

data class BienReponse(
    val id: Int,
    val bailleur_id: Int,
    val titre: String,
    val pays: String,
    val ville: String,
    val quartier: String,
    val type: String,
    val capacite: Int,
    val loyer: String
)

data class DonneesPays(val drapeau: String, val villes: Map<String, List<String>>)

data class RequeteContrat(val bienId: Int, val locataireId: Int, val montantLoyer: String, val dateDebut: String)
data class ContratReponse(val id: Int, val bien_titre: String?, val montant_loyer: String, val signe_bailleur: Boolean, val signe_locataire: Boolean)

data class RequeteMessage(val destinataireId: Int, val contenu: String)
data class MessageReponse(val id: Int, val expediteur_id: Int, val destinataire_id: Int, val contenu: String, val date_envoi: String)
data class ConversationReponse(val utilisateur_id: Int, val nom_complet: String, val dernier_message: String, val date_envoi: String)

interface ApiService {
    @POST("auth/inscription")
    suspend fun inscription(@Body requete: RequeteInscription): Response<AuthReponse>

    @POST("auth/connexion")
    suspend fun connexion(@Body requete: RequeteConnexion): Response<AuthReponse>

    @GET("biens")
    suspend fun rechercherBiens(
        @Query("pays") pays: String?,
        @Query("ville") ville: String?,
        @Query("quartier") quartier: String?
    ): Response<List<BienReponse>>

    @Multipart
    @POST("biens")
    suspend fun ajouterBien(
        @Header("Authorization") token: String,
        @Part("titre") titre: RequestBody,
        @Part("pays") pays: RequestBody,
        @Part("ville") ville: RequestBody,
        @Part("quartier") quartier: RequestBody,
        @Part("type") type: RequestBody,
        @Part("capacite") capacite: RequestBody,
        @Part("loyer") loyer: RequestBody,
        @Part("description") description: RequestBody,
        @Part photos: List<MultipartBody.Part>
    ): Response<Map<String, Any>>

    @GET("localisations")
    suspend fun recupererLocalisations(): Response<Map<String, DonneesPays>>

    @POST("contrats")
    suspend fun creerContrat(@Header("Authorization") token: String, @Body requete: RequeteContrat): Response<ContratReponse>

    @GET("contrats")
    suspend fun listerContrats(@Header("Authorization") token: String): Response<List<ContratReponse>>

    @POST("contrats/{id}/signer")
    suspend fun signerContrat(@Header("Authorization") token: String, @Path("id") id: Int): Response<ContratReponse>

    @GET("messages/conversations")
    suspend fun recupererConversations(@Header("Authorization") token: String): Response<List<ConversationReponse>>

    @GET("messages/{autreId}")
    suspend fun recupererFilMessages(@Header("Authorization") token: String, @Path("autreId") autreId: Int): Response<List<MessageReponse>>

    @POST("messages")
    suspend fun envoyerMessageApi(@Header("Authorization") token: String, @Body requete: RequeteMessage): Response<MessageReponse>
}