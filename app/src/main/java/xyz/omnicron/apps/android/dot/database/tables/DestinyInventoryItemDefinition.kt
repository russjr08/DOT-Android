package xyz.omnicron.apps.android.dot.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DestinyInventoryItemDefinition")
data class DestinyDatabaseDefinition(
    @PrimaryKey
    val id: Int,
    val json: String
)