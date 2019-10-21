package xyz.omnicron.apps.android.dot

import android.app.Application
import com.downloader.PRDownloader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import xyz.omnicron.apps.android.dot.api.Destiny


class App: Application() {

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

    val appModule = module {
        single { Destiny(androidContext()) }
    }

}