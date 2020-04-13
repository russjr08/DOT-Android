package xyz.omnicron.apps.android.dot.api

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit.JSONConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.omnicron.apps.android.dot.api.interfaces.DestinyService
import xyz.omnicron.apps.android.dot.api.interfaces.IResponseReceiver
import xyz.omnicron.apps.android.dot.api.models.ManifestResponse
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse
import java.util.*

class Destiny(ctx: Context): Interceptor {

    private val destinyApi: DestinyService
    private val prefs: SharedPreferences


    init {

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(this)

        val client = httpClient.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(JSONConverterFactory.create())
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
    fun refreshAccessToken(callback: IResponseReceiver<OAuthResponse>){
        //TODO: Implement persisting new set of tokens
        val refreshToken = prefs.getString("refreshToken", "INVALID") as String
        val call = destinyApi.retrieveTokens("", Constants.CLIENT_ID, "refresh_token", Constants.CLIENT_SECRET, refreshToken)

        call.enqueue(object : Callback<OAuthResponse> {
            override fun onFailure(call: Call<OAuthResponse>, t: Throwable) {
                callback.onNetworkFailure(call, t)
            }

            override fun onResponse(call: Call<OAuthResponse>, response: Response<OAuthResponse>) {
                callback.onNetworkTaskFinished(response, call)

                if(response.isSuccessful && response.body() != null) {

                    val accessExpiresIn = Date()
                    accessExpiresIn.time = accessExpiresIn.time + (3600 * 1000)

                    val refreshExpiresIn = Date()
                    refreshExpiresIn.time = refreshExpiresIn.time + (3600 * 1000)

                    prefs.edit().putString("accessToken", response.body()?.accessToken).apply()
                    prefs.edit().putLong("accessTokenExpires", accessExpiresIn.time).apply()

                    prefs.edit().putString("refreshToken", response.body()?.refreshToken).apply()
                    prefs.edit().putLong("refreshTokenExpires", refreshExpiresIn.time).apply()
                }
            }

        })
    }

    fun retrieveManifest(callback: IResponseReceiver<ManifestResponse>) {
        val call = destinyApi.retrieveManifest()

        call.enqueue(object: Callback<ManifestResponse> {
            override fun onFailure(call: Call<ManifestResponse>, t: Throwable) {
                callback.onNetworkFailure(call, t)
            }

            override fun onResponse(call: Call<ManifestResponse>, response: Response<ManifestResponse>) {
                callback.onNetworkTaskFinished(response, call)
            }

        })
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()

        val request = original.newBuilder()
            .header("X-API-KEY", Constants.API_KEY)
            .method(original.method, original.body)
            .build()

        return chain.proceed(request)

    }

    class Constants {

        companion object {
            val API_KEY = "ef8699ee753947409bab21607d63c3bb"

            val CLIENT_SECRET = "sWw9MbOXK4LlyvOpoCxkiojDP.bQq0NEob-bVBJ1mtM"
            val CLIENT_ID = "29831"

            val BASE_URL = "https://www.bungie.net/Platform/"

            val LOGIN_ENDPOINT = "https://www.bungie.net/en/oauth/authorize?client_id=${CLIENT_ID}&response_type=code"
        }

    }


}