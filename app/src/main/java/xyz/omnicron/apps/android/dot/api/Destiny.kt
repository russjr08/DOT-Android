package xyz.omnicron.apps.android.dot.api

import android.content.Context
import android.content.SharedPreferences
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.omnicron.apps.android.dot.api.interfaces.DestinyService
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse
import java.text.DateFormat
import java.util.*

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
        return destinyApi.retrieveTokens(code, Constants.CLIENT_ID, "authorization_code", Constants.CLIENT_SECRET, "")
    }

    /**
     * Checks to see if the access token for the application (stored in SharedPreferences)
     * should be valid, by checking the saved "expires in" time.
     *
     * @return If the access token has been determined as "should be" valid.
     */

    fun isAccessValid(): Boolean {

        val now = Date()

        val accessExpireDate = Date(prefs.getLong("accessTokenExpires", 0))


        return accessExpireDate > now
    }

    /**
     * Checks to see if the refresh token for the application (stored in SharedPreferences)
     * <b><i>should</i></b> be valid, by checking the saved "expires in" time.
     *
     * @return If the refresh token has been determined as "should be" valid.
     */
    fun isRefreshValid(): Boolean {
        val now = Date()

        val refreshExpireDate = Date(prefs.getLong("refreshTokenExpires", 0))

        return refreshExpireDate > now
    }


    /**
     * Reaches out to the Bungie token endpoint to exchange our refresh token for a new set of tokens.
     */
    fun refreshAccessToken(): Call<OAuthResponse> {
        //TODO: Implement persisting new set of tokens
        val refreshToken = prefs.getString("refreshToken", "INVALID") as String
        return destinyApi.retrieveTokens("", Constants.CLIENT_ID, "refresh_token", Constants.CLIENT_SECRET, refreshToken)
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