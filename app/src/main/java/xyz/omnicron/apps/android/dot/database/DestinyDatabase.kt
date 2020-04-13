package xyz.omnicron.apps.android.dot.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

    /**
     * This function attempts to lookup an item in the Destiny manifest, given the table name,
     * and the item's hash/id.
     *
     * @param hash The id of the item to be looked up.
     * @param table The table in the Destiny Manifest where this item can be found
     * @return A generic, barebones, representation of the item from the Manifest.
     */
    fun getDestinyDatabaseItemFromHash(hash: Int, table: String): DestinyDatabaseItem? {

        val cursor = this.openDatabase().rawQuery("SELECT * FROM ${table} WHERE id + 4294967296 = ${hash} OR id = ${hash}", null)

        // Database lookups are expensive. If the item has already been looked up and cached,
        // return the entry from cache instead.
        if(DestinyDatabaseCache.cache.containsKey(hash)) {
            return DestinyDatabaseCache.cache.get(hash)
        }

        if(cursor.moveToFirst()) {
            val item = Klaxon().parse<DestinyDatabaseItem>(cursor.getString(1))

            // Cache the item since it was not previously cached
            item?.let { DestinyDatabaseCache.cache.put(hash, it) }
            this.logger.log(Level.INFO, "Added an entry into database cache: ${item?.displayProperties?.name}")
        }

        return null
    }

}

class DestinyDatabaseCache {
    companion object {
        var cache: HashMap<Int, DestinyDatabaseItem> = HashMap()
    }
}

class DestinyDatabaseItem(
    var displayProperties: DisplayProperties,
    var hash: Int,
    var redacted: Boolean,
    var itemTypeDisplayName: String,
    var itemTypeAndTierDisplayName: String
)

class DisplayProperties(
    var description: String,
    var icon: String,
    var name: String,
    var hasIcon: Boolean

)