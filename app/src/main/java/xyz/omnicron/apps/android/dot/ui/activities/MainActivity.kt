package xyz.omnicron.apps.android.dot.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.jakewharton.processphoenix.ProcessPhoenix
import com.squareup.picasso.Picasso
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Response
import xyz.omnicron.apps.android.dot.*
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.api.interfaces.IApiResponseCallback
import xyz.omnicron.apps.android.dot.api.interfaces.IResponseReceiver
import xyz.omnicron.apps.android.dot.api.models.DestinyMembership
import xyz.omnicron.apps.android.dot.api.models.OAuthResponse
import xyz.omnicron.apps.android.dot.database.DestinyDatabase
import xyz.omnicron.apps.android.dot.databinding.ActivityMainBinding
import xyz.omnicron.apps.android.dot.databinding.NavHeaderMainBinding
import xyz.omnicron.apps.android.dot.ui.pursuits.IPursuitsView
import xyz.omnicron.apps.android.dot.ui.pursuits.PursuitsFragment
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var app: App
    private lateinit var prefs: SharedPreferences

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHeaderBinding: NavHeaderMainBinding

    val BUNGIE_NET_BASE = "https://www.bungie.net"

    private val destiny: Destiny by inject()


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        navHeaderBinding = NavHeaderMainBinding.bind(binding.navView.getHeaderView(0))

        feedbackRegion.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/russjr08/DOT-Android/issues"))
            startActivity(browserIntent)
        }

        prefs = getSharedPreferences("dot", Context.MODE_PRIVATE)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_pursuits
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



        this.app = application as App

        destiny.database = DestinyDatabase(this, prefs.getString("manifestName", "").orEmpty())

        destiny.refreshAccessToken(object : IResponseReceiver<OAuthResponse> {
            override fun onNetworkTaskFinished(response: Response<OAuthResponse>,request: Call<OAuthResponse>) {
                promptForUserMembershipChoice().andThen(destiny.updateDestinyProfile()).subscribe({
                    val fragment = supportFragmentManager.currentNavigationFragment
                    if(fragment is PursuitsFragment) {
                        (fragment as IPursuitsView).onReadyToStart()
                    }
                }) { error ->
                    if(error is DestinyAuthException) {
                        returnToLoginActivity(error)
                    } else {
                        Log.e("DOT Authentication", error.toString())
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("That shouldn't have happened...")
                            .setMessage(error.toString())
                            .setPositiveButton("Restart App") { dialog, _ ->
                                dialog.dismiss()
                                ProcessPhoenix
                                    .triggerRebirth(this@MainActivity,
                                    Intent(this@MainActivity,
                                        LoginActivity::class.java))
                            }.show()
                    }
                }

            }

            override fun onNetworkFailure(request: Call<OAuthResponse>, error: Throwable) {
                Log.e("DOT Authentication", error.toString())
                MaterialAlertDialogBuilder(baseContext)
                    .setTitle("Failed To Authenticate With Bungie...")
                    .setMessage(error.toString())
                    .setPositiveButton("Acknowledge") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }.show()
            }
        })

        updateNavHeader()

    }


    // TODO: Reimplement authentication validity checking
    private fun checkLoginIsValid(): Completable {
        return Completable.create { subscriber ->
            if(!destiny.isAccessValid()) {
                if(destiny.isRefreshValid()) {
                    destiny.refreshAccessToken(
                        callback = object: IResponseReceiver<OAuthResponse> {
                        override fun onNetworkTaskFinished(response: Response<OAuthResponse>,
                                                           request: Call<OAuthResponse>) {
                            Log.d("DOT Auth Refresh", "Updated Authentication Tokens")
                            subscriber.onComplete()
                        }

                        override fun onNetworkFailure(request: Call<OAuthResponse>,
                                                      error: Throwable) {
                            if(error is DestinyException) {
                                subscriber.onError(error)
                            } else {
                                subscriber.onError(Exception("A network error has occurred."))
                            }
                        }

                    })
                } else {
                    subscriber.onError(DestinyAuthException("All tokens have expired."))
                }
            }
            subscriber.onComplete()
        }
    }

    @SuppressLint("ApplySharedPref")
    @Suppress("NAME_SHADOWING")
    private fun returnToLoginActivity(error: Throwable?) {
        val listener = DialogInterface.OnClickListener { _, _ ->
            prefs.edit().clear().commit()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        if(error?.message != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.popup_auth_expired_error_title))
                .setMessage(resources.getString(R.string.popup_auth_expired_error_body)
                    .replace("%s".toRegex(), error.toString()))
                .setPositiveButton(R.string.popup_auth_expired_button_title, listener)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.popup_auth_expired_error_title))
                .setMessage(resources.getString(R.string.popup_auth_expired_error_body)
                    .replace("%s".toRegex(), resources.getString(R.string.popup_auth_expired_no_error)))
                .setPositiveButton(R.string.popup_auth_expired_button_title, listener)
                .show()
        }
    }

    private fun promptForUserMembershipChoice(): Completable {
        return Completable.create { subscriber ->
            if(prefs.contains("membershipId") || prefs.contains("membershipType")) {
                subscriber.onComplete()
            } else {
                destiny.getUserMemberships(callback = object :
                    IApiResponseCallback<Array<DestinyMembership>> {
                    override fun onRequestSuccess(data: Array<DestinyMembership>) {

                        if (data.size == 1) {
                            prefs.edit().putLong("membershipId", data[0].membershipId).apply()
                            prefs.edit().putInt("membershipType", data[0].membershipType.value)
                                .apply()
                            subscriber.onComplete()
                            return
                        } else {
                            val platformNames = arrayListOf<String>()
                            for (membership in data) {
                                platformNames.add("${membership.membershipType.getNameFromType()} (${membership.displayName})")
                            }

                            @Suppress("UNCHECKED_CAST")
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(resources.getString(R.string.popup_platform_title))
                                .setItems(platformNames.toTypedArray()) { _, which ->
                                    Log.d("DOT", "Platform Selection: ${data[which].membershipType}")
                                    prefs.edit().putLong("membershipId", data[which].membershipId).apply()
                                    prefs.edit()
                                        .putInt("membershipType", data[which].membershipType.value).apply()
                                    subscriber.onComplete()
                                }
                                .show()
                        }



                    }

                    override fun onRequestFailed(error: Throwable) {
                        subscriber.onError(error)
                    }
                })
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateNavHeader() {
        destiny.updateBungieUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                navHeaderBinding.name.text = destiny.bungieNetUser.displayName
                navHeaderBinding.aboutText.text = destiny.bungieNetUser.about

                val formatter = SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US)

                val creationDate = resources.getText(R.string.nav_header_firstAccess).replace(
                    "%s".toRegex(), formatter.format(destiny.bungieNetUser.firstAccess!!))
                navHeaderBinding.creationDate.text = creationDate
                val lastUpdate = resources.getText(R.string.nav_header_lastUpdate).replace(
                    "%s".toRegex(), formatter.format(destiny.bungieNetUser.lastUpdate!!))
                navHeaderBinding.lastUpdateDate.text = lastUpdate

                Picasso.with(this@MainActivity)
                    .load(BUNGIE_NET_BASE + destiny.bungieNetUser.profilePicturePath)
                    .noFade()
                    .into(navHeaderBinding.imageView)
            }, {
                TODO("Display an error to the user when the profile update was unsuccessful")
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menu[0].setOnMenuItemClickListener {
//            Snackbar.make(this.nav_view, "Settings is not ready yet, check back soon! :)", Snackbar.LENGTH_LONG).show()
            val settingsIntent = Intent(this, AppSettingsActivity::class.java)
            startActivity(settingsIntent)
            return@setOnMenuItemClickListener true
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
