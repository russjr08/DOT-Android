package xyz.omnicron.apps.android.dot.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import xyz.omnicron.apps.android.dot.database.dao.DatabaseDao
import xyz.omnicron.apps.android.dot.database.tables.DestinyDatabaseDefinition
import java.sql.Blob

@Database(entities = [DestinyDatabaseDefinition::class], version = 1)
@TypeConverters(Converters::class)
abstract class DestinyDatabase : RoomDatabase() {

    abstract fun inventoryItemDao(): DatabaseDao

    companion object {
        @JvmField
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

}

class Converters {
    @TypeConverter
    fun fromBlob(value: Blob?): String? {
        return String(value?.binaryStream?.readBytes()!!)
    }

    @TypeConverter
    fun toBlob(value: String?): Blob? {
        return null
    }
}