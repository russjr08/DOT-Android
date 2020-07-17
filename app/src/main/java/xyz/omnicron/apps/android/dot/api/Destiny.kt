package xyz.omnicron.apps.android.dot.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.joda.time.Instant
import org.joda.time.LocalDateTime
import org.json.JSONObject
import retrofit.JSONConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.omnicron.apps.android.dot.DestinyParseException
import xyz.omnicron.apps.android.dot.api.interfaces.DestinyService
import xyz.omnicron.apps.android.dot.api.interfaces.IApiResponseCallback
import xyz.omnicron.apps.android.dot.api.interfaces.IResponseReceiver
import xyz.omnicron.apps.android.dot.api.models.*
import xyz.omnicron.apps.android.dot.database.DestinyDatabase
import java.text.SimpleDateFormat
import java.util.*

@Suppress("NAME_SHADOWING")
class Destiny(ctx: Context): Interceptor {

    private val destinyApi: DestinyService
    private val prefs: SharedPreferences

    lateinit var bungieNetUser: BungieNetUser
    lateinit var destinyProfile: DestinyProfile
    lateinit var database: DestinyDatabase

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

    fun getSavedMembershipId(): String {
        return prefs.getLong("membershipId", 0).toString()
    }

    fun getSavedMembershipType(): MembershipType {
        return MembershipType.from(prefs.getInt("membershipType", -1))
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

//        val accessExpireDate = Date(prefs.getLong("accessTokenExpires", 0))

        val accessExpireDate = LocalDateTime.parse(prefs.getString("accessTokenExpires", Instant.EPOCH.toString()))

        return accessExpireDate > LocalDateTime.now()
    }

    /**
     * Checks to see if the refresh token for the application (stored in SharedPreferences)
     * <b><i>should</i></b> be valid, by checking the saved "expires in" time.
     *
     * @return If the refresh token has been determined as "should be" valid.
     */
    fun isRefreshValid(): Boolean {
//        val refreshExpireDate = Date(prefs.getLong("refreshTokenExpires", 0))

        val refreshExpireDate = LocalDateTime.parse(prefs.getString("refreshTokenExpires", Instant.EPOCH.toString()))

        return refreshExpireDate > LocalDateTime.now()
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
                    val oAuthResponse = response.body() as OAuthResponse

                    val accessExpiresIn = LocalDateTime.now()
                    accessExpiresIn.plusSeconds(oAuthResponse.accessTokenExpiresIn)

                    val refreshExpiresIn = LocalDateTime.now()
                    refreshExpiresIn.plusSeconds(oAuthResponse.refreshTokenExpiresIn)

                    prefs.edit().putString("accessToken", oAuthResponse.accessToken).apply()
                    prefs.edit().putString("accessTokenExpires", accessExpiresIn.toString()).apply()

                    prefs.edit().putString("refreshToken", oAuthResponse.refreshToken).apply()
                    prefs.edit().putString("refreshTokenExpires", refreshExpiresIn.toString()).apply()
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


    /**
     * This function returns a list of available platforms the signed in user plays on
     * (or rather, linked to the signed in Bungie.net profile)
     */
    fun getUserMemberships(callback: IApiResponseCallback<Array<DestinyMembership>>) {
        val memberships = arrayListOf<DestinyMembership>()

        val call = destinyApi.retrieveMemberships(prefs.getInt("bngMembershipId", 0))

        call.enqueue(object: Callback<JSONObject> {
            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                if(response.isSuccessful) {
                    val resObject = response.body()
                    resObject.let { jsonRoot ->
                        val responseRoot = jsonRoot?.getJSONObject("Response")
                        responseRoot?.let { responseRoot ->
                            val memberships = arrayListOf<DestinyMembership>()
                            val membershipNodes = responseRoot.getJSONArray("destinyMemberships")
                            for(i in 0 until membershipNodes.length()) {
                                val membershipNode = membershipNodes[i] as JSONObject
                                Log.d("DOT", membershipNode.toString())
                                memberships.add(DestinyMembership(
                                    displayName = membershipNode.getString("displayName"),
                                    iconPath = membershipNode.getString("iconPath"),
                                    membershipId = membershipNode.getString("membershipId").toLong(),
                                    membershipType = MembershipType.from(membershipNode.getInt("membershipType"))
                                ))
                            }

                            callback.onRequestSuccess(memberships.toTypedArray())
                        }

                    }
                }
            }

        })

    }

    fun updateBungieUser(): Completable {
        return Completable.create { subscriber ->
            val call = destinyApi.retrieveMemberships(prefs.getInt("bngMembershipId", 0))

            call.enqueue(object: Callback<JSONObject> {
                override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                    subscriber.onError(t)
                }

                override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                    if(response.isSuccessful) {
                        val responseObj = response.body()?.getJSONObject("Response")
                        responseObj.let { response ->
                            val bungieNetUserNode = response?.getJSONObject("bungieNetUser")
                            bungieNetUserNode?.let { userNode ->
                                val bungieNetUser = BungieNetUser(
                                    displayName = userNode.getString("displayName"),
                                    about = userNode.getString("about"),
                                    firstAccess = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(userNode.getString("firstAccess")),
                                    lastUpdate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(userNode.getString("lastUpdate")),
                                    membershipId = userNode.getLong("membershipId"),
                                    profilePicturePath = userNode.getString("profilePicturePath"),
                                    profileThemeName = userNode.getString("profileThemeName")
                                )

                                this@Destiny.bungieNetUser = bungieNetUser
                                subscriber.onComplete()
                                return
                            }
                            subscriber.onError(DestinyParseException("The response from the API was unable to be parsed correctly."))
                        }
                    }
                }

            })
        }

    }

    fun updateDestinyProfile(): Completable {
        return Completable.create { subscriber ->
            val call = destinyApi.retrieveProfile(getSavedMembershipType().value, getSavedMembershipId(),
                listOf(100, 200))

            call.enqueue(object: Callback<JSONObject> {
                override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                    subscriber.onError(t)
                }

                override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                    Log.d("DOT", response.body().toString())
                    if(response.isSuccessful) {

                        // Parse basic profile information
                        val responseObj = response.body()!!.getJSONObject("Response")
                        val profileObj = responseObj.getJSONObject("profile")
                        val profileData = profileObj.getJSONObject("data")
                        val userInfo = profileData.getJSONObject("userInfo")
                        this@Destiny.destinyProfile = DestinyProfile(MembershipType.from(userInfo.getInt("membershipType")),
                        userInfo.getString("membershipId"), userInfo.getString("displayName"))

                        // After basic profile information has been updated, get basic character data
                        val characterNode = responseObj.getJSONObject("characters")
                        val characterData = characterNode.getJSONObject("data")
                        for(characterId in characterData.keys()) {
                            val characterObj = characterData[characterId] as JSONObject
                            val character = DestinyCharacter(
                                membershipId = characterObj.getString("membershipId"),
                                membershipType = MembershipType.from(characterObj.getInt("membershipType")),
                                characterId = characterObj.getString("characterId"),
                                dateLastPlayed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(characterObj.getString("dateLastPlayed"))!!,
                                light = characterObj.getInt("light"),
                                emblemHash = characterObj.getLong("emblemHash"),
                                emblemPath = characterObj.getString("emblemPath"),
                                emblemBackgroundPath = characterObj.getString("emblemBackgroundPath"),
                                race = DestinyRace.from(characterObj.getInt("raceType")),
                                gender = DestinyGender.from(characterObj.getInt("genderType")),
                                classType = DestinyClass.from(characterObj.getInt("classType"))
                            )
                            this@Destiny.destinyProfile.characters.add(character)
                        }
                        subscriber.onComplete()
                    } else {
                        subscriber.onError(DestinyParseException("The response from the API was unable to be parsed correctly."))
                    }
                }

            })
        }

    }

    fun retrieveCharacterData(characterId: String, components: List<Int>): Observable<JSONObject> {
        return Observable.create { emitter ->
            val call = destinyApi.retrieveCharacter(getSavedMembershipType().value,
                getSavedMembershipId(), characterId, components)

            call.enqueue(object: Callback<JSONObject> {
                override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                    emitter.onError(t)
                }

                override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                    if(response.isSuccessful) {
                        val responseObj = (response.body() as JSONObject).getJSONObject("Response")
                        emitter.onNext(responseObj)
                        emitter.onComplete()
                    } else {
                        emitter.onError(DestinyParseException("The response from the API was unable to be parsed correctly."))
                    }
                }

            })
        }
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