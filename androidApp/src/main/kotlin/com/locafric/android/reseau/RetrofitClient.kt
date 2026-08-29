package com.locafric.android.reseau

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Adresse de ton backend déployé sur Vercel
    private const val URL_BASE = "https://locafric.vercel.app/api/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}