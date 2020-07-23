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
    fun retrieveMemberships(@Header("Authorization") bearer: String, @Path("membershipID") id: Int): Call<JSONObject>

    @GET("Destiny2/{membershipType}/Profile/{membershipId}/Character/{characterId}")
    fun retrieveCharacter(@Header("Authorization") bearer: String,
                        @Path("membershipType") membershipType: Int,
                        @Path("membershipId") membershipId: String,
                        @Path("characterId") characterId: String,
                        @Query("components") components: List<Int>): Call<JSONObject>

    @GET("Destiny2/{membershipType}/Profile/{membershipId}")
    fun retrieveProfile(@Header("Authorization") bearer: String,
                        @Path("membershipType") membershipType: Int,
                        @Path("membershipId") membershipId: String,
                        @Query("components") components: List<Int>): Call<JSONObject>
}