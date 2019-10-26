package xyz.omnicron.apps.android.dot

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.downloader.PRDownloader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.database.DestinyDatabase


class App: Application() {

    lateinit var database: DestinyDatabase

    override fun onCreate() {
        PRDownloader.initialize(applicationContext)
        super.onCreate()

        // Start Koin DI
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }


    }

    fun initializeDatabase() {
        val prefs = getSharedPreferences("dot", Context.MODE_PRIVATE)
        val manifestName = prefs.getString("manifestName", "")

        if (manifestName.isNullOrEmpty()) {
            TODO("Manifest name should not be empty when calling this function")
        }

        val dbFile = getDatabasePath(manifestName)

        database = Room.databaseBuilder(this,
            DestinyDatabase::class.java, "$manifestName"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries() // TODO: Move to a separate thread
            .addMigrations(DestinyDatabase.MIGRATION_1_2)
            .build()

    }

    val appModule = module {
        single { Destiny(androidContext()) }
    }

}