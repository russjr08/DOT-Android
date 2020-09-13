package xyz.omnicron.apps.android.dot.api.models

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
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
            destinyApi.retrieveCharacterData(this.characterId, listOf(200,202,102,201,205,301))
                .subscribeOn(Schedulers.newThread())
                .subscribe { data ->
                    Thread(Runnable {
                        this.pursuits.clear()

                        // Parse items from "equipment", which is currently equipped items
                        data.getJSONObject("equipment").let { equipmentNode ->
                            val dataNode = equipmentNode.getJSONObject("data")
                            val itemsArray = dataNode.getJSONArray("items")
                            addPursuitFromNode(itemsArray, destinyApi)
                        }

                        // Parse items from "inventory"
                        data.getJSONObject("inventory").let { inventoryNode ->
                            val dataNode = inventoryNode.getJSONObject("data")
                            val itemsArray = dataNode.getJSONArray("items")
                            addPursuitFromNode(itemsArray, destinyApi)
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

                        this.pursuits = stripPursuitsWithZeroObjectives(this.pursuits)
                        this.pursuits = stripCompletedPursuits(this.pursuits)
                        subscriber.onComplete()
                    }).start()
                }
        }
    }

    private fun addPursuitFromNode(itemsArray: JSONArray, destinyApi: Destiny) {
        // Begin parsing basic item entries from API
        for(i in 0 until itemsArray.length()) {
            val itemNode = itemsArray[i] as JSONObject
            val armorBuckets = arrayOf(1585787867, 14239492, 20886954, 3448274439, 3551918588)
            if(itemNode.getInt("bucketHash") != 1345459588 && !armorBuckets.contains(itemNode.getLong("bucketHash"))) {
                continue
            }
            val dbItem = destinyApi.database.getDestinyDatabaseItemFromHash(itemNode.getInt("itemHash"),
                "DestinyInventoryItemDefinition")
            dbItem?.let { databaseItem ->
                if(itemNode.has("expirationDate")) {
                    val pursuit = DestinyPursuit(databaseItem = databaseItem,
                        instanceId = itemNode.getString("itemInstanceId"),
                        quantity = itemNode.getInt("quantity"),
                        expirationDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(itemNode.getString("expirationDate")),
                        bucketHash = itemNode.getInt("bucketHash")
                    )
                    this.pursuits.add(pursuit)

                } else {
                    val pursuit = DestinyPursuit(databaseItem = databaseItem,
                        instanceId = itemNode.optString("itemInstanceId"),
                        quantity = itemNode.getInt("quantity"),
                        expirationDate = null,
                        bucketHash = itemNode.getInt("bucketHash")
                    )
                    this.pursuits.add(pursuit)

                }

            }
        }
    }

    private fun stripPursuitsWithZeroObjectives(pursuits: ArrayList<DestinyPursuit>): ArrayList<DestinyPursuit> {
        val cleanedPursuits = arrayListOf<DestinyPursuit>()

        for(pursuit in pursuits) {
            if(pursuit.objectives.size > 0) {
                cleanedPursuits.add(pursuit)
            }
        }

        return cleanedPursuits
    }

    private fun stripCompletedPursuits(pursuits: ArrayList<DestinyPursuit>): ArrayList<DestinyPursuit> {
        val cleanedPursuits = arrayListOf<DestinyPursuit>()

        for(pursuit in pursuits) {
            var hasAnIncompleteObjective = false
            for(objective in pursuit.objectives) {
                if(!objective.complete) {
                    hasAnIncompleteObjective = true
                    break
                }
            }

            if(hasAnIncompleteObjective) {
                cleanedPursuits.add(pursuit)
            }
        }

        return cleanedPursuits
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