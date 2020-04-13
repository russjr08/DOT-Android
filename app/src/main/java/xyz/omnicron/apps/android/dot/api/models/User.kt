package xyz.omnicron.apps.android.dot.api.models

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
        fun from(findValue: Int): MembershipType = MembershipType.values().first { it.value == findValue }
    }
}