package xyz.omnicron.apps.android.dot.api.models

import xyz.omnicron.apps.android.dot.database.DestinyDatabaseRecord

/**
 * @see "https://bungie-net.github.io/multi/schema_Destiny-Components-Records-DestinyRecordComponent.html#schema_Destiny-Components-Records-DestinyRecordComponent"
 */
data class DestinyRecord(
    val record: DestinyDatabaseRecord,
    val objectives: ArrayList<DestinyObjectiveData> = arrayListOf()
) {

    enum class DestinyRecordState(val state: Int) {
        NONE(0),
        REDEEMED(1),
        UNAVAILABLE(2),
        OBJECTIVE_NOT_COMPLETED(4),
        OBSCURED(8),
        INVISIBLE(16),
        ENTITLEMENT_UNOWNED(32),
        CAN_EQUIP_TITLE(64)
    }

    fun isSeasonalChallenge(): Boolean {

        return true
    }

}

