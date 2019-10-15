package xyz.omnicron.apps.android.dot

import android.app.Application
import com.downloader.PRDownloader


class App: Application() {

    override fun onCreate() {
        PRDownloader.initialize(applicationContext)
        super.onCreate()
    }
}