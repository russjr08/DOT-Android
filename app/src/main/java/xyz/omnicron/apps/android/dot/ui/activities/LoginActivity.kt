package xyz.omnicron.apps.android.dot.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.downloader.*
import kotlinx.android.synthetic.main.activity_login.*
import org.joda.time.LocalDateTime
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.zeroturnaround.zip.ZipUtil
import retrofit2.Call
import retrofit2.Response
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.api.interfaces.IResponseReceiver
import xyz.omnicron.apps.android.dot.api.models.ManifestResponse
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse
import java.util.logging.Logger
import kotlin.math.ceil

class LoginActivity : AppCompatActivity(),
    ILoginHandler, IResponseReceiver<ManifestResponse>, OnProgressListener, OnDownloadListener {

    var tabConnection: CustomTabsServiceConnection? = null
    var loginPopup: MaterialDialog? = null
    var manifestPopup: MaterialDialog? = null

    var preferences: SharedPreferences? = null

    var manifestVersionPending: String? = null

    val destiny: Destiny by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        preferences = getSharedPreferences("dot", Context.MODE_PRIVATE)

        loginBtn.setOnClickListener {
            launchTab(this, Uri.parse(Destiny.Constants.LOGIN_ENDPOINT))
        }

        loginPopup = MaterialDialog(this)

        val openUri = intent.data
        val code = openUri?.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            Log.d("DOT", "Retrieved OAuth Code: $code")
            showLoginInProcess()

            LoginTask(this).execute(code)
        }

        // If a login token is detected, skip requesting authorization.
        preferences?.let {
            if(it.contains("accessToken")) {
                checkAndDownloadManifest()
            }
        }

    }

    private fun launchTab(context: Context, uri: Uri) {
        tabConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                componentName: ComponentName,
                client: CustomTabsClient
            ) {
                val builder = CustomTabsIntent.Builder()
                val intent = builder.setToolbarColor(ContextCompat.getColor(baseContext,
                    R.color.colorPrimary
                ))
                                .setStartAnimations(baseContext, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                .build()
                intent.intent.flags = FLAG_ACTIVITY_NEW_TASK // This prevents activity going to background after redirection
                client.warmup(0L)
                intent.launchUrl(context, uri)
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome",
            tabConnection as CustomTabsServiceConnection
        )
    }

    override fun onDestroy() {
        if(tabConnection != null) {
            unbindService(tabConnection as CustomTabsServiceConnection)
        }
        super.onDestroy()
    }

    private fun showLoginInProcess() {
        loginPopup?.customView(R.layout.popup_login_progress)
        loginPopup?.title(R.string.popup_login_progress_title)
        loginPopup?.icon(R.drawable.lock_clock)
        loginPopup?.cancelable(false)
        loginPopup?.cancelOnTouchOutside(false)
        loginPopup?.show()
    }

    override fun onLoginAttemptFailed() {
        Logger.getLogger("DOT").info("Login attempt failed! :(")
        loginPopup?.dismiss()

        MaterialDialog(this).show {
            title(R.string.popup_login_failed_title)
            message(R.string.popup_login_failed_text)
            icon(R.drawable.alert_circle)
        }
    }

    override fun onLoginFinished(response: OAuthResponse) {
        Logger.getLogger("DOT").info(response.toString())

        /** Access + Refresh tokens come with an "expiresIn", which is the amount of time in seconds
         * after it was obtained.
         */

        // Access Token
        preferences?.edit()?.putString("accessToken", response.accessToken)?.apply()
        var accessExpiresIn = LocalDateTime.now()
        accessExpiresIn = accessExpiresIn.plusSeconds(3600)
        preferences?.edit()?.putString("accessTokenExpires", accessExpiresIn.toString())?.apply()
        
        // Refresh Token
        preferences?.edit()?.putString("refreshToken", response.refreshToken)?.apply()
        var refreshExpiresIn = LocalDateTime.now()
        refreshExpiresIn = refreshExpiresIn.plusSeconds(7776000)
        preferences?.edit()?.putString("refreshTokenExpires", refreshExpiresIn.toString())?.apply()

        preferences?.edit()?.putInt("bngMembershipId", response.bngMembershipId)?.apply()

        loginPopup?.dismiss()

        checkAndDownloadManifest()

    }

    private fun exitLoginActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        finish()
    }

    private fun checkAndDownloadManifest() {

        manifestPopup = MaterialDialog(this).show {
            title(R.string.popup_manifest_update_check_title)
            message(R.string.popup_manifest_update_check)
            noAutoDismiss()
        }

        destiny.retrieveManifest(this)
    }

    override fun onNetworkTaskFinished(response: Response<ManifestResponse>, request: Call<ManifestResponse>) {

        preferences?.let {
            val preferences = it

            if(response.isSuccessful) {
                val manifestResponse = response.body() ?: TODO("Not Implemented Yet")
                val manifest = manifestResponse.data

                if(preferences.getString("manifestVersion", "NONE") != manifest.version) {
                    // Download the latest game database (manifest)
                    manifestVersionPending = manifest.version

                    manifestPopup?.title(R.string.popup_manifest_downloading_title)
                    manifestPopup?.message(null, getString(R.string.popup_manifest_downloading).replace("%p", "0"))
                    val baseURL = "https://www.bungie.net"

                    if(manifest.contentPaths["en"] == null) {
                        TODO("Implement manifest check failure")
                    }

                    val manifestURL = baseURL + manifest.contentPaths["en"]

                    val manifestPathArray = manifest.contentPaths["en"]?.split("/")
                    val manifestName = manifestPathArray?.get(manifestPathArray.size - 1)

                    preferences.edit().putString("manifestName", manifestName).apply()

                    PRDownloader.download(manifestURL, filesDir.absolutePath, "manifest.zip")
                        .build()
                        .setOnProgressListener(this)
                        .start(this)
                } else {
                    // Manifest is already downloaded and up-to-date, carry on!
                    manifestPopup?.dismiss()
                    exitLoginActivity()
                }

            }
        }


    }

    override fun onNetworkFailure(request: Call<ManifestResponse>, error: Throwable) {
        manifestPopup?.dismiss()
        MaterialDialog(this).show {
            title(R.string.popup_manifest_download_failure_title)
            if(error.localizedMessage != null) {
                val errString = getString(R.string.popup_manifest_download_failure).replace(
                    "%e",
                    error.localizedMessage as String
                )

                message(null, errString)
            } else {
                val errString = getString(R.string.popup_manifest_download_failure).replace(
                    "%e",
                    "Unknown Error"
                )

                message(null, errString)
            }
        }
    }

    // Manifest download management

    override fun onProgress(progress: Progress?) {
        if(progress == null) return
        val currentAsDouble = progress.currentBytes.toDouble()
        val totalAsDouble = progress.totalBytes.toDouble()
        val percentage = ceil(currentAsDouble / totalAsDouble * 100.0)
        this.runOnUiThread {
            this.manifestPopup?.message(null, getString(R.string.popup_manifest_downloading).replace("%p", percentage.toString()))
        }
    }

    override fun onDownloadComplete() {
        println("Download finished!")
        manifestPopup?.dismiss()
        // Flag Manifest as Downloaded
        preferences?.edit()?.putString("manifestVersion", manifestVersionPending)?.apply()

        val manifestName = preferences?.getString("manifestName", "")
        if(manifestName.isNullOrEmpty()) {
            TODO("Manifest is empty")
        }

        // Manifest needs to be unzipped after downloading
        ZipUtil.unpack(getFileStreamPath("manifest.zip"), getDatabasePath(manifestName).parentFile)

        exitLoginActivity()
    }

    override fun onError(error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

interface ILoginHandler {
    /**
     * This function is called when a login attempt is completed, and a non-error HTTP code is
     * returned from Bungie
     */
    fun onLoginFinished(response: OAuthResponse)

    /**
     * This function is called when a login attempt to Bungie fails with an HTTP error code.
     */
    fun onLoginAttemptFailed()
}

class LoginTask(val handler: ILoginHandler): AsyncTask<String, Void, Response<OAuthResponse>>(), KoinComponent {

    override fun doInBackground(vararg code: String?): Response<OAuthResponse> {
        val destiny: Destiny by inject()
        Logger.getLogger("DOT").info("Login attempt inbound!")
        return destiny.retrieveTokens(code[0] as String).execute()
    }

    override fun onPostExecute(response: Response<OAuthResponse>) {

        if(response.isSuccessful) {
            handler.onLoginFinished(response.body() as OAuthResponse)
        } else {
            handler.onLoginAttemptFailed()
        }
    }

}
