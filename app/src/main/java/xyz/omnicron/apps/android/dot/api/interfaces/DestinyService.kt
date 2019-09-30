package xyz.omnicron.apps.android.dot.api.interfaces

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import xyz.omnicron.apps.android.dot.api.models.ManifestResponse
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse

interface DestinyService {

    @FormUrlEncoded
    @POST("app/oauth/token/")
    fun retrieveTokens(@Field("code") code: String, @Field("client_id") clientId: String,
                       @Field("grant_type") grantType: String,
                       @Field("client_secret") clientSecret: String,
                       @Field("refresh_token") refreshToken: String): Call<OAuthResponse>

    @GET("Destiny2/Manifest/")

    fun retrieveManifest(): Call<ManifestResponse>

}