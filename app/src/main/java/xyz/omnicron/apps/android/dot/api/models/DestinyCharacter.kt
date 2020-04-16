package xyz.omnicron.apps.android.dot.api.models

import xyz.omnicron.apps.android.dot.api.Destiny
import java.util.*
import kotlin.collections.ArrayList

class DestinyCharacter(var membershipId: String,
                       var membershipType: MembershipType,
                       var characterId: String,
                       var dateLastPlayed: Date,
                       var light: Int,
                       var emblemHash: Long,
                       var emblemPath: String,
                       var emblemBackgroundPath: String,
                       var race: DestinyRace,
                       var gender: DestinyGender,
                       var classType: DestinyClass,
                       var pursuits: ArrayList<DestinyItem> = arrayListOf()
) {
    fun updateCharacter(destinyApi: Destiny) {

    }
}

enum class DestinyRace(val id: Int) {
    AWOKEN(1), EXO(2), HUMAN(0);

    fun getNameFromType(): String {
        return when(this) {
            AWOKEN -> "Awoken"
            EXO -> "Exo"
            HUMAN -> "Human"
        }
    }

    companion object {
        fun from(findValue: Int): DestinyRace = values().first { it.id == findValue }
    }

}


enum class DestinyClass(val id: Int) {
    WARLOCK(2), HUNTER(1), TITAN(0);

    fun getNameFromType(): String {
        return when(this) {
            WARLOCK -> "Warlock"
            HUNTER -> "Hunter"
            TITAN -> "Titan"
        }
    }

    companion object {
        fun from(findValue: Int): DestinyClass = values().first { it.id == findValue }
    }
}

enum class DestinyGender(val id: Int) {
    MALE(0), FEMALE(1);

    fun getNameFromType(): String {
        return when(this) {
            MALE -> "Male"
            FEMALE -> "Female"
        }
    }

    companion object {
        fun from(findValue: Int): DestinyGender = values().first { it.id == findValue }
    }
}