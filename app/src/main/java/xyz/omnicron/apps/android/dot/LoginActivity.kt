package xyz.omnicron.apps.android.dot

import android.content.ComponentName
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_login.*
import xyz.omnicron.apps.android.dot.api.Destiny




class LoginActivity : AppCompatActivity() {

    var tabConnection: CustomTabsServiceConnection? = null

    var preferences: SharedPreferences = getSharedPreferences("dot", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginBtn.setOnClickListener {
            launchTab(this, Uri.parse(Destiny.Constants.LOGIN_ENDPOINT))
        }

        val openUri = intent.data
        val code = openUri?.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            Log.d("DOT", "Retrieved OAuth Code: $code")

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
        unbindService(tabConnection as CustomTabsServiceConnection)
        super.onDestroy()
    }

}
