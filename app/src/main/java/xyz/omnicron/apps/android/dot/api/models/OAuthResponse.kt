package xyz.omnicron.apps.android.dot.api.models

import com.squareup.moshi.Json

class OAuthResponse (
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val accessTokenExpiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "refresh_expires_in") val refreshTokenExpiresIn: Int,
    @Json(name = "membership_id") val bngMembershipId: Int
 )