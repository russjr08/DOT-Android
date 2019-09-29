package xyz.omnicron.apps.android.dot.api

import android.content.Context
import android.content.SharedPreferences
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.omnicron.apps.android.dot.api.interfaces.DestinyService
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse

public class Destiny(ctx: Context) {

    private val destinyApi: DestinyService
    private val prefs: SharedPreferences

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        destinyApi = retrofit.create(DestinyService::class.java)

        prefs = ctx.getSharedPreferences("dot", Context.MODE_PRIVATE)
    }

    fun retrieveTokens(code: String): Call<OAuthResponse> {
        return destinyApi.retrieveTokens(code, Constants.CLIENT_ID, "authorization_code", Constants.CLIENT_SECRET)
    }

    fun isAccessValid(): Boolean {



        return false
    }

    public class Constants {

        companion object {
            val API_KEY = "ef8699ee753947409bab21607d63c3bb"

            val CLIENT_SECRET = "sWw9MbOXK4LlyvOpoCxkiojDP.bQq0NEob-bVBJ1mtM"
            val CLIENT_ID = "29831"

            val BASE_URL = "https://www.bungie.net/Platform/"

            val LOGIN_ENDPOINT = "https://www.bungie.net/en/oauth/authorize?client_id=${CLIENT_ID}&response_type=code"
        }

    }


}