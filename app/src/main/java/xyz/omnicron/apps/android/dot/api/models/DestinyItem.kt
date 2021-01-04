package xyz.omnicron.apps.android.dot.api.models

import xyz.omnicron.apps.android.dot.database.DestinyDatabaseItem
import xyz.omnicron.apps.android.dot.database.DestinyDatabaseObjective
import java.util.*

open class DestinyItem(var databaseItem: DestinyDatabaseItem,
                       var instanceId: String,
                       var quantity: Int = 1,
                       var transferStatus: Int = 0,
                       var bucketHash: Int) {


    fun isPursuit(): Boolean {
        return this.bucketHash == 1345459588
    }
}

class DestinyPursuit(databaseItem: DestinyDatabaseItem,
                    instanceId: String, quantity: Int, val expirationDate: Date?, transferStatus: Int, bucketHash: Int
) : DestinyItem(databaseItem, instanceId, quantity, transferStatus, bucketHash) {

    val objectives = arrayListOf<DestinyObjectiveData>()
    val rewards = arrayListOf<DestinyDatabaseItem>()

}

data class DestinyObjectiveData(var objectiveDefinition: DestinyDatabaseObjective, var progress: Int, var complete: Boolean, val visible: Boolean)