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
    val titre: String,
    val pays: String,
    val ville: String,
    val quartier: String,
    val type: String,
    val capacite: Int,
    val loyer: String
)

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
}