package xyz.omnicron.apps.android.dot.api.interfaces

import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*
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

    @GET("User/GetMembershipsById/{membershipID}/-1/")
    fun retrieveMemberships(@Path("membershipID") id: Int): Call<JSONObject>

}