package xyz.omnicron.apps.android.dot.api.models

import java.util.*

class UserProfile {
}


enum class MembershipType(val value: Int) {
    STEAM(3), STADIA(5), XBOX_LIVE(1), PLAYSTATION_NETWORK(2);

    fun getNameFromType(): String {
        return when(this) {
            STEAM -> "Steam"
            STADIA -> "Stadia"
            XBOX_LIVE -> "Xbox Live"
            PLAYSTATION_NETWORK -> "PlayStation Network"
        }
    }

    companion object {
        fun from(findValue: Int): MembershipType = values().first { it.value == findValue }
    }
}

data class BungieNetUser(
    val displayName: String,
    val about: String,
    val firstAccess: Date?,
    val lastUpdate: Date?,
    val membershipId: Long,
    val profilePicturePath: String,
    val profileThemeName: String
)