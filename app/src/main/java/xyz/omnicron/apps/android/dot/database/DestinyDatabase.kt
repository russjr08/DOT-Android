package xyz.omnicron.apps.android.dot.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.util.logging.Level
import java.util.logging.Logger


class DestinyDatabase(val ctx: Context, val DB_NAME: String): SQLiteOpenHelper(ctx, ctx.getDatabasePath(DB_NAME).path, null, 1) {

    val logger = Logger.getLogger("DOT Database")

    fun openDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(ctx.getDatabasePath(DB_NAME).path, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    // @TODO Convert to RX (ensure not running on main thread)
    /**
     * This function attempts to lookup an item in the Destiny manifest, given the table name,
     * and the item's hash/id.
     *
     * @param hash The id of the item to be looked up.
     * @param table The table in the Destiny Manifest where this item can be found
     * @return A generic, barebones, representation of the item from the Manifest.
     */
    fun getDestinyDatabaseItemFromHash(hash: Int, table: String): DestinyDatabaseItem? {
        // Database lookups are expensive. If the item has already been looked up and cached,
        // return the entry from cache instead.
        if(DestinyDatabaseCache.itemCache.containsKey(hash)) {
            return DestinyDatabaseCache.itemCache[hash]
        }

        val database = this.openDatabase()
        val cursor = database.rawQuery("SELECT * FROM ${table} WHERE id + 4294967296 = ${hash} OR id = ${hash}", null)

        if(cursor.moveToFirst()) {
            val item = Klaxon().parse<DestinyDatabaseItem>(cursor.getString(1))

            // Cache the item since it was not previously cached
            item?.let { DestinyDatabaseCache.itemCache.put(hash, it) }
            this.logger.log(Level.INFO, "Added an entry into database cache: ${item?.displayProperties?.name}")
            cursor.close()
            database.close()
            return item
        }

        return null
    }

    fun getDestinyDatabaseObjectiveFromHash(hash: Int, table: String): DestinyDatabaseObjective? {
        // Database lookups are expensive. If the item has already been looked up and cached,
        // return the entry from cache instead.
        if(DestinyDatabaseCache.objectivesCache.containsKey(hash)) {
            return DestinyDatabaseCache.objectivesCache[hash]
        }

        val database = this.openDatabase()
        val cursor = database.rawQuery("SELECT * FROM ${table} WHERE id + 4294967296 = ${hash} OR id = ${hash}", null)

        if(cursor.moveToFirst()) {
            val item = Klaxon().parse<DestinyDatabaseObjective>(cursor.getString(1))

            // Cache the item since it was not previously cached
            item?.let { DestinyDatabaseCache.objectivesCache.put(hash, it) }
            this.logger.log(Level.INFO, "Added an objective into database cache: ${item?.progressDescription}")
            cursor.close()
            database.close()
            return item
        }

        return null
    }

}

class DestinyDatabaseCache {
    companion object {
        var itemCache: HashMap<Int, DestinyDatabaseItem> = HashMap()
        var objectivesCache: HashMap<Int, DestinyDatabaseObjective> = HashMap()
    }
}

class DestinyDatabaseItem(
    var displayProperties: DisplayProperties?,
    var hash: Long?,
    var redacted: Boolean?,
    var itemTypeDisplayName: String?,
    var itemTypeAndTierDisplayName: String?,
    @Json("value") var rewards: DestinyDatabaseRewards? = null
)

class DestinyDatabaseRewards(
    @Json(name = "itemValue") var entries: Array<DestinyDatabaseRewardEntry> = emptyArray()
)

class DestinyDatabaseRewardEntry(
    var itemHash: Long = 0,
    var quantity: Int = 0
)

class DestinyDatabaseObjective(
    var hash: Long,
    var redacted: Boolean,
    var completionValue: Int,
    var showValueOnComplete: Boolean,
    var progressDescription: String
)


class DisplayProperties(
    var description: String? = "",
    var icon: String? = "",
    var name: String? = ""
) {
    override fun toString(): String {
        return "DisplayProperty: $name // $description"
    }
}