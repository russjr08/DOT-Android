package xyz.omnicron.apps.android.dot.api.models

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import xyz.omnicron.apps.android.dot.api.Destiny
import java.text.SimpleDateFormat
import java.util.*

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
                       var pursuits: ArrayList<DestinyPursuit> = arrayListOf()
) {
    fun updatePursuits(destinyApi: Destiny): Completable {
        return Completable.create { subscriber ->
            destinyApi.retrieveCharacterData(this.characterId, listOf(200,202,102,201,301))
                .subscribeOn(Schedulers.io())
                .subscribe { data ->
                    val inventoryNode = data.getJSONObject("inventory")
                    val dataNode = inventoryNode.getJSONObject("data")
                    val itemsArray = dataNode.getJSONArray("items")

                    // Begin parsing basic item entries from API
                    for(i in 0 until itemsArray.length()) {
                        val itemNode = itemsArray[i] as JSONObject
                        if(itemNode.getInt("bucketHash") != 1345459588) {
                            continue
                        }
                        val dbItem = destinyApi.database.getDestinyDatabaseItemFromHash(itemNode.getInt("itemHash"),
                            "DestinyInventoryItemDefinition")
                        dbItem?.let { datbaseItem ->
                            if(itemNode.has("expirationDate")) {
                                val pursuit = DestinyPursuit(databaseItem = datbaseItem,
                                    instanceId = itemNode.getString("itemInstanceId"),
                                    quantity = itemNode.getInt("quantity"),
                                    expirationDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(itemNode.getString("expirationDate")),
                                    bucketHash = itemNode.getInt("bucketHash")
                                )
                                this.pursuits.add(pursuit)

                            } else {
                                val pursuit = DestinyPursuit(databaseItem = datbaseItem,
                                    instanceId = itemNode.optString("itemInstanceId"),
                                    quantity = itemNode.getInt("quantity"),
                                    expirationDate = null,
                                    bucketHash = itemNode.getInt("bucketHash")
                                )
                                this.pursuits.add(pursuit)

                            }

                        }
                    }

                    // Begin parsing item components data (such as pursuit objectives)
                    val componentNode = data.getJSONObject("itemComponents")
                    val objectivesNode = componentNode.getJSONObject("objectives")
                    val objectivesDataNode = objectivesNode.getJSONObject("data")

                    for(instanceId in objectivesDataNode.keys()) {
                        val objectivesArray = (objectivesDataNode[instanceId] as JSONObject).getJSONArray("objectives")
                        val position = this.pursuits.indexOfFirst { it.instanceId == instanceId }
                        if(position == -1){
                            continue
                        }
                        for(i in 0 until objectivesArray.length()) {
                            val objectiveNode = objectivesArray[i] as JSONObject
                            val objectiveDefinition = destinyApi.database.getDestinyDatabaseObjectiveFromHash(objectiveNode.getInt("objectiveHash"), "DestinyObjectiveDefinition")
                            if(objectiveDefinition != null) {
                                this.pursuits[position].objectives.add(DestinyObjectiveData(
                                    objectiveDefinition = objectiveDefinition,
                                    progress = objectiveNode.getInt("progress"),
                                    complete = objectiveNode.getBoolean("complete"),
                                    visible = objectiveNode.getBoolean("visible")
                                ))
                            }

                        }
                    }
                    subscriber.onComplete()
                }
        }
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