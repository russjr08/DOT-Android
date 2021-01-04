package xyz.omnicron.apps.android.dot.api.models

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.database.DestinyDatabase
import xyz.omnicron.apps.android.dot.ui.activities.AppSettingsActivity
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
) : KoinComponent {

    private val destiny: Destiny by inject()

    private val armorBuckets: Array<Long> = arrayOf(1585787867, 14239492, 20886954, 3448274439, 3551918588)


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
                            loopThroughPotentialPursuits(itemsArray, destinyApi)
                        }


                        // Parse items from "inventory"
                        data.getJSONObject("inventory").let { inventoryNode ->
                            val dataNode = inventoryNode.getJSONObject("data")
                            val itemsArray = dataNode.getJSONArray("items")
                            loopThroughPotentialPursuits(itemsArray, destinyApi)
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

                        if(!destiny.getAppPreferences().getBoolean("pref_pursuits_show_zero_objectives", AppSettingsActivity.PREFS_PURSUITS_DEFAULT_SHOW_ZERO_OBJECTIVES)) {
                            this.pursuits = stripPursuitsWithZeroObjectives(this.pursuits)
                        }
                        if(!destiny.getAppPreferences().getBoolean("pref_pursuits_show_completed", AppSettingsActivity.PREFS_PURSUITS_DEFAULT_SHOW_COMPLETED)) {
                            this.pursuits = stripCompletedPursuits(this.pursuits)
                        } else {
                            this.pursuits = stripEmptyArmor(this.pursuits)
                        }

                        this.pursuits = addRewardsToPursuits(this.pursuits, destinyApi.database)
                        subscriber.onComplete()
                    }).start()
                }
        }
    }

    private fun loopThroughPotentialPursuits(itemsArray: JSONArray, destinyApi: Destiny) {
        for(i in 0 until itemsArray.length()) {
            val itemNode = itemsArray[i] as JSONObject
            if(itemNode.getInt("bucketHash") == 1345459588) {
                addPursuitFromNode(itemNode, destinyApi)
            } else if(this.armorBuckets.contains(itemNode.getLong("bucketHash"))) {
                val armorPreference = destiny.getAppPreferences().getString("pref_pursuits_show_armor_pursuits", "never")!!
                if(armorPreference == "all_armor_present") {
                    addPursuitFromNode(itemNode, destinyApi)
                } else if(armorPreference == "equipped_only") {
                    if(itemNode.getInt("transferStatus") == 1) {
                        addPursuitFromNode(itemNode, destinyApi)
                    }
                }
            }
        }
    }

    private fun addPursuitFromNode(itemNode: JSONObject, destinyApi: Destiny) {
        // Begin parsing basic item entries from API
        val dbItem = destinyApi.database.getDestinyDatabaseItemFromHash(itemNode.getInt("itemHash"),
            "DestinyInventoryItemDefinition")
        dbItem?.let { databaseItem ->
            if(itemNode.has("expirationDate")) {
                val pursuit = DestinyPursuit(databaseItem = databaseItem,
                    instanceId = itemNode.getString("itemInstanceId"),
                    quantity = itemNode.getInt("quantity"),
                    expirationDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(itemNode.getString("expirationDate")),
                    transferStatus = itemNode.getInt("transferStatus"),
                    bucketHash = itemNode.getInt("bucketHash")
                )
                this.pursuits.add(pursuit)

            } else {
                val pursuit = DestinyPursuit(databaseItem = databaseItem,
                    instanceId = itemNode.optString("itemInstanceId"),
                    quantity = itemNode.getInt("quantity"),
                    expirationDate = null,
                    transferStatus = itemNode.getInt("transferStatus"),
                    bucketHash = itemNode.getInt("bucketHash")
                )
                this.pursuits.add(pursuit)

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

    private fun stripEmptyArmor(pursuits: ArrayList<DestinyPursuit>): ArrayList<DestinyPursuit> {
        val cleanedPursuits = arrayListOf<DestinyPursuit>()

        for(pursuit in pursuits) {
            if(!this.armorBuckets.contains(pursuit.bucketHash.toLong()) && pursuit.objectives.size != 0) {
                cleanedPursuits.add(pursuit)
            }
        }

        return cleanedPursuits
    }

    private fun addRewardsToPursuits(pursuits: ArrayList<DestinyPursuit>, database: DestinyDatabase): ArrayList<DestinyPursuit> {
        val updatedPursuits = arrayListOf<DestinyPursuit>()

        for(pursuit in pursuits) {
            if(pursuit.databaseItem.rewards?.entries?.isNotEmpty() == true) {
                for (reward in pursuit.databaseItem.rewards!!.entries) {
                    val rewardItemDefinition = database.getDestinyDatabaseItemFromHash(reward.itemHash.toInt(), "DestinyInventoryItemDefinition")
                    rewardItemDefinition?.let { item ->
                        pursuit.rewards.add(item)
                    }
                }
            }
            updatedPursuits.add(pursuit)
        }

        return updatedPursuits
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