package xyz.omnicron.apps.android.dot.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Observable
import xyz.omnicron.apps.android.dot.database.tables.DestinyDatabaseDefinition

@Dao
interface DatabaseDao {

    @Query("SELECT * FROM DestinyInventoryItemDefinition")
    fun getAllItemDefinitions(): Observable<DestinyDatabaseDefinition>

}