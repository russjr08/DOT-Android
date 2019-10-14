package xyz.omnicron.apps.android.dot

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
import kotlinx.android.synthetic.main.activity_login.*
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse
import java.util.*
import java.util.logging.Logger

class LoginActivity : AppCompatActivity(), ILoginHandler {


    var tabConnection: CustomTabsServiceConnection? = null
    var loginPopup: MaterialDialog? = null

    var preferences: SharedPreferences? = null

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

            LoginTask(this, this.applicationContext).execute(code)
        }

    }

    private fun launchTab(context: Context, uri: Uri) {
        tabConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                componentName: ComponentName,
                client: CustomTabsClient
            ) {
                val builder = CustomTabsIntent.Builder()
                val intent = builder.setToolbarColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
                                .setStartAnimations(baseContext, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                .build()
                intent.intent.flags = FLAG_ACTIVITY_NEW_TASK // This prevents activity going to background after redirection
                client.warmup(0L)
                intent.launchUrl(context, uri)
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", tabConnection)
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

        // Access Token
        preferences?.edit()?.putString("accessToken", response.accessToken)?.apply()
        val accessExpiresIn = Date()
        accessExpiresIn.time = accessExpiresIn.time + (3600 * 1000)
        preferences?.edit()?.putLong("accessTokenExpires", accessExpiresIn.time)?.apply()
        
        // Refresh Token
        preferences?.edit()?.putString("refreshToken", response.refreshToken)?.apply()
        val refreshExpiresIn = Date()
        refreshExpiresIn.time = refreshExpiresIn.time + (3600 * 1000)
        preferences?.edit()?.putLong("refreshTokenExpires", refreshExpiresIn.time)?.apply()

        preferences?.edit()?.putInt("bngMembershipId", response.bngMembershipId)?.apply()

        loginPopup?.dismiss()

        exitLoginActivity()

    }

    private fun exitLoginActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        finish()
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

class LoginTask(val handler: ILoginHandler, val ctx: Context): AsyncTask<String, Void, Void>() {

    override fun doInBackground(vararg code: String?): Void? {
        if (ctx.get() == null) {
            return null
        }

        val destiny = Destiny(ctx.get()!!)
        Logger.getLogger("DOT").info("Login attempt inbound!")
        val response = destiny.retrieveTokens(code[0] as String).execute()

        if(response.isSuccessful) {
            handler.onLoginFinished(response.body() as OAuthResponse)
        } else {
            handler.onLoginAttemptFailed()
        }

        return null
    }

}
