package xyz.omnicron.apps.android.dot.api.models

import java.util.*
import kotlin.collections.ArrayList

class DestinyProfile(var membershipType: MembershipType,
                     var membershipId: String,
                     var displayName: String
) {
    var characters: ArrayList<DestinyCharacter> = arrayListOf()

    fun getLastPlayedCharacterId(): String {
        val mostRecentDate = characters.stream().map { char -> char.dateLastPlayed}.max(Date::compareTo).get()
        return characters.stream().filter { char -> char.dateLastPlayed == mostRecentDate }.findFirst().get().characterId
    }

    fun getCharacterById(id: String): DestinyCharacter {
        return characters.stream().filter { char -> char.characterId == id}.findFirst().get()
    }

}