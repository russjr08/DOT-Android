package xyz.omnicron.apps.android.dot.api.models

import xyz.omnicron.apps.android.dot.database.DestinyDatabaseItem
import java.util.*

open class DestinyItem(var databaseItem: DestinyDatabaseItem,
                       var instanceId: String,
                       var quantity: Int = 1,
                       var expirationDate: Date?,
                       var bucketHash: Int) {


    fun isPursuit(): Boolean {
        return this.bucketHash == 1345459588
    }
}

class DestinyPursuit(databaseItem: DestinyDatabaseItem,
                    instanceId: String, quantity: Int, expirationDate: Date?, bucketHash: Int
) : DestinyItem(databaseItem, instanceId, quantity, expirationDate, bucketHash) {

    val objectives = arrayListOf<DestinyObjectiveData>()

}

data class DestinyObjectiveData(var objectiveHash: Int, var progress: Int, var completionValue: Int, var complete: Boolean, val visible: Boolean)