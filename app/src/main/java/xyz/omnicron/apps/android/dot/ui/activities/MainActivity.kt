package xyz.omnicron.apps.android.dot.ui.activities

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_main.view.*
import org.koin.android.ext.android.inject
import xyz.omnicron.apps.android.dot.App
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.api.interfaces.IApiResponseCallback
import xyz.omnicron.apps.android.dot.api.models.BungieNetUser
import xyz.omnicron.apps.android.dot.api.models.DestinyMembership
import xyz.omnicron.apps.android.dot.database.DestinyDatabase
import xyz.omnicron.apps.android.dot.databinding.ActivityMainBinding
import xyz.omnicron.apps.android.dot.databinding.NavHeaderMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var app: App
    private lateinit var prefs: SharedPreferences

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHeaderBinding: NavHeaderMainBinding

    val BUNGIE_NET_BASE = "https://www.bungie.net"

    val destiny: Destiny by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        navHeaderBinding = NavHeaderMainBinding.bind(binding.navView.getHeaderView(0))


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

        val destinyDatabase = DestinyDatabase(this, prefs.getString("manifestName", "").orEmpty())

        if(!prefs.contains("membershipId")) {
            promptForUserMembershipChoice()
        }

        updateNavHeader()

    }

    private fun promptForUserMembershipChoice() {
        destiny.getUserMemberships(callback = object : IApiResponseCallback<Array<DestinyMembership>> {
            override fun onRequestSuccess(data: Array<DestinyMembership>) {

                if(data.size > 1) {
                    prefs.edit().putLong("membershipId", data[0].membershipId).apply()
                    prefs.edit().putInt("membershipType", data[0].membershipType.value).apply()
                }

                val platformNames = arrayListOf<String>()
                for(membership in data) {
                    platformNames.add("${membership.membershipType.getNameFromType()} (${membership.displayName})")
                }

                @Suppress("UNCHECKED_CAST")
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(resources.getString(R.string.popup_platform_title))
                    .setItems(platformNames.toTypedArray()) { _, which ->
                        Log.d("DOT", "Platform Selection: ${data[which].membershipType}")
                        prefs.edit().putLong("membershipId", data[which].membershipId)
                        prefs.edit().putInt("membershipType", data[which].membershipType.value)
                    }
                    .show()

            }

            override fun onRequestFailed(error: Throwable) {
                TODO("Not yet implemented")
            }


        })
        
        

    }

    private fun updateNavHeader() {
        destiny.getBungieNetUser(callback = object : IApiResponseCallback<BungieNetUser> {
            override fun onRequestSuccess(data: BungieNetUser) {
                runOnUiThread {
                    navHeaderBinding.name.text = data.displayName
                    navHeaderBinding.aboutText.text = data.about

                    val formatter = SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US)

                    val creationDate = resources.getText(R.string.nav_header_firstAccess).replace(
                        "%s".toRegex(), formatter.format(data.firstAccess!!))
                    navHeaderBinding.creationDate.text = creationDate
                    val lastUpdate = resources.getText(R.string.nav_header_lastUpdate).replace(
                        "%s".toRegex(), formatter.format(data.lastUpdate!!))
                    navHeaderBinding.lastUpdateDate.text = lastUpdate

                    Picasso.with(this@MainActivity)
                        .load(BUNGIE_NET_BASE + data.profilePicturePath)
                        .noFade()
                        .into(navHeaderBinding.imageView)

                }
            }

            override fun onRequestFailed(error: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
