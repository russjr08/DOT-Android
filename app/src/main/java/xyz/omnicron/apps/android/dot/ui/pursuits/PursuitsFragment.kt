package xyz.omnicron.apps.android.dot.ui.pursuits

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.ProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.pursuits_fragment.*
import org.koin.android.ext.android.inject
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.Destiny
import xyz.omnicron.apps.android.dot.api.models.DestinyCharacter
import xyz.omnicron.apps.android.dot.api.models.DestinyClass

class PursuitsFragment : Fragment() {

    private val destiny: Destiny by inject()


    private lateinit var characterFab: SpeedDialView
    private lateinit var nothingLayout: LinearLayout
    private lateinit var refreshProgressBar: ProgressIndicator

    private lateinit var selectedCharacterId: String

    private lateinit var refreshTimer: CountDownTimer
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var pursuitsAdapter: PursuitsAdapter

    private lateinit var swipeContainer: SwipeRefreshLayout

    companion object {
        fun newInstance() = PursuitsFragment()
    }

    private lateinit var viewModel: PursuitsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pursuits_fragment, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        characterFab = view.findViewById(R.id.fab_character_selection)

        swipeContainer = view.findViewById(R.id.swipeContainer)

        nothingLayout = view.findViewById(R.id.nothingFoundContainer)

        refreshProgressBar = view.findViewById(R.id.refreshProgressBar)

        linearLayoutManager = LinearLayoutManager(activity)
        pursuitsContainer.layoutManager = linearLayoutManager
        pursuitsAdapter = PursuitsAdapter()
        pursuitsContainer.adapter = pursuitsAdapter

        swipeContainer.setProgressViewOffset(false, 0, 150)
        swipeContainer.setOnRefreshListener { refreshCharacters() }

        destiny.updateDestinyProfile().observeOn(AndroidSchedulers.mainThread()).subscribe({
            Snackbar.make(pursuitsFrameLayout, "Initial profile update completed; Looking for Pursuits...", Snackbar.LENGTH_LONG).show()
            setSelectedCharacter(destiny.destinyProfile.getLastPlayedCharacterId())
            setOnCharacterSelect()

            refreshCharacters()
            startRefreshTimer()
        },
        { error ->

            Log.e("DOT-Initialization", "Error with initial pursuits check. The error given was ${error.localizedMessage}")
            Snackbar.make(pursuitsFrameLayout, "An error occurred trying to check for bounties, try a manual refresh", Snackbar.LENGTH_LONG).show()
        })

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PursuitsViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun startRefreshTimer() {
        val refreshInterval = 30000
        refreshProgressBar.isIndeterminate = false
        refreshProgressBar.max = refreshInterval
        refreshProgressBar.progress = refreshInterval

        refreshTimer = object : CountDownTimer(refreshInterval.toLong(), 1000) {
            override fun onFinish() {
                refreshCharacters()
                val animator = ObjectAnimator.ofInt(refreshProgressBar, "progress", refreshInterval).setDuration(3000)
                animator.doOnEnd {
                    refreshTimer.start()
                }
                animator.start()
            }

            override fun onTick(millisUntilFinished: Long) {
                val percentage: Double = (millisUntilFinished.toDouble() / refreshInterval.toDouble()) * refreshInterval
                ObjectAnimator.ofInt(refreshProgressBar, "progress", percentage.toInt()).setDuration(2000).start()
            }

        }.start()
    }

    private fun refreshCharacters() {
        // TODO - Show refresh icon
        swipeContainer.isRefreshing = true
        destiny.destinyProfile.characters.forEach { character ->
            character.updatePursuits(destiny).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io()).subscribe {
                    Snackbar.make(pursuitsFrameLayout, "Finished updating ${character.classType.getNameFromType()}", Snackbar.LENGTH_LONG).show()
                    if(!this::selectedCharacterId.isInitialized) {
                        selectedCharacterId = destiny.destinyProfile.getLastPlayedCharacterId()
                    }
                    if(character.characterId == selectedCharacterId) {
                        pursuitsAdapter.setPursuitsList(character.pursuits)
                        pursuitsAdapter.notifyDataSetChanged()
                        swipeContainer.isRefreshing = false
                    }
                    showListEmptyGraphicIfEmpty()
                }
        }
    }

    private fun showListEmptyGraphicIfEmpty() {
//        val mainHandler = Handler(requireContext().mainLooper)
//        val runnable = Runnable {
//            if(getSelectedCharacter().pursuits.size == 0) {
//                nothingLayout.visibility = View.VISIBLE
//            } else {
//                nothingLayout.visibility = View.GONE
//            }
//        }
//        mainHandler.post(runnable)

    }

    private fun setOnCharacterSelect() {
        characterFab.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_warlock -> {
                    setSelectedCharacter(destiny.destinyProfile.characters.stream().filter{ character -> character.classType == DestinyClass.WARLOCK }.findFirst().get().characterId)
                    characterFab.close()
                    pursuitsAdapter.setPursuitsList(getSelectedCharacter().pursuits)
                    pursuitsAdapter.notifyDataSetChanged()
                    showListEmptyGraphicIfEmpty()
                    return@OnActionSelectedListener true // false will close it without animation
                }
                R.id.fab_hunter -> {
                    setSelectedCharacter(destiny.destinyProfile.characters.stream().filter{ character -> character.classType == DestinyClass.HUNTER }.findFirst().get().characterId)
                    characterFab.close()
                    pursuitsAdapter.setPursuitsList(getSelectedCharacter().pursuits)
                    pursuitsAdapter.notifyDataSetChanged()
                    showListEmptyGraphicIfEmpty()
                    return@OnActionSelectedListener true
                }
                R.id.fab_titan -> {
                    setSelectedCharacter(destiny.destinyProfile.characters.stream().filter{ character -> character.classType == DestinyClass.TITAN }.findFirst().get().characterId)
                    characterFab.close()
                    pursuitsAdapter.setPursuitsList(getSelectedCharacter().pursuits)
                    pursuitsAdapter.notifyDataSetChanged()
                    showListEmptyGraphicIfEmpty()
                    return@OnActionSelectedListener true
                }
            }
            false
        })
    }

    private fun getSelectedCharacter(): DestinyCharacter {
        return destiny.destinyProfile.characters.first { character -> character.characterId == selectedCharacterId }
    }

    private fun setSelectedCharacter(id: String) {
        selectedCharacterId = id
        val character = destiny.destinyProfile.getCharacterById(id)
        Snackbar.make(pursuitsFrameLayout, "Selected ${character.classType.getNameFromType()}", Snackbar.LENGTH_SHORT).show()


        characterFab.clearActionItems()

        characterFab.setMainFabOpenedDrawable(resources.getDrawable(getResourceIdForClass(destiny.destinyProfile.getCharacterById(selectedCharacterId).classType), null))
        characterFab.setMainFabClosedDrawable(resources.getDrawable(getResourceIdForClass(destiny.destinyProfile.getCharacterById(selectedCharacterId).classType), null))

        val actionItems = arrayListOf<SpeedDialActionItem>()
        val selectedCharacterType = character.classType

        for(type in DestinyClass.values()) {
            when(type) {
                DestinyClass.TITAN -> {
                    if(selectedCharacterType != DestinyClass.TITAN) {
                        actionItems.add(SpeedDialActionItem.Builder(
                            R.id.fab_titan, getResourceIdForClass(DestinyClass.TITAN))
                            .setLabel("Titan")
                            .setFabBackgroundColor(resources.getColor(R.color.design_default_color_background, null))
                            .create()
                        )
                    }
                }

                DestinyClass.HUNTER -> {
                    if(selectedCharacterType != DestinyClass.HUNTER) {
                        actionItems.add(
                            SpeedDialActionItem.Builder(
                            R.id.fab_hunter, getResourceIdForClass(DestinyClass.HUNTER))
                            .setLabel("Hunter")
                            .setFabBackgroundColor(resources.getColor(R.color.design_default_color_background, null))
                            .create()
                        )
                    }

                }

                DestinyClass.WARLOCK -> {
                    if(selectedCharacterType != DestinyClass.WARLOCK) {
                        actionItems.add(SpeedDialActionItem.Builder(
                            R.id.fab_warlock, getResourceIdForClass(DestinyClass.WARLOCK))
                            .setLabel("Warlock")
                            .setFabBackgroundColor(resources.getColor(R.color.design_default_color_background, null))
                            .create()
                        )
                    }

                }
            }
        }

        characterFab.addAllActionItems(actionItems)


    }




    private fun getResourceIdForClass(classType: DestinyClass): Int {
        return when(classType) {
            DestinyClass.WARLOCK -> R.drawable.ic_warlock_symbol_original
            DestinyClass.HUNTER -> R.drawable.ic_hunter_symbol_original
            DestinyClass.TITAN -> R.drawable.ic_titan_symbol_original
        }
    }

}
