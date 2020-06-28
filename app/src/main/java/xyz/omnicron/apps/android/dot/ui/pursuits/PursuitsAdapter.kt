package xyz.omnicron.apps.android.dot.ui.pursuits

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.models.DestinyPursuit
import xyz.omnicron.apps.android.dot.inflate
import xyz.omnicron.apps.android.dot.ui.objectives.ObjectivesAdapter

class PursuitsAdapter: RecyclerView.Adapter<PursuitsAdapter.PursuitHolder>() {

    private var pursuits = arrayListOf<DestinyPursuit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PursuitsAdapter.PursuitHolder {
        val inflatedView = parent.inflate(R.layout.pursuit_card, false)
        return PursuitHolder(inflatedView)
    }

    override fun getItemCount() = pursuits.size

    override fun onBindViewHolder(holder: PursuitsAdapter.PursuitHolder, position: Int) {
        val pursuit = pursuits[position]
        holder.bindPursuit(pursuit, holder.itemView.context)
    }

    fun setPursuitsList(pursuits: ArrayList<DestinyPursuit>) {
        this.pursuits = pursuits
    }

    class PursuitHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {


        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            Log.d("DOT PursuitsContainer", "Pursuit item clicked!")
        }

        fun bindPursuit(pursuit: DestinyPursuit, ctx: Context) {
            // Initialize layout with properties
            val pursuitTitleText: TextView = this.itemView.findViewById(R.id.pursuitTitleText)
            val pursuitDescriptionText: TextView = this.itemView.findViewById(R.id.pursuitDescriptionText)
            val pursuitTypeText: TextView = this.itemView.findViewById(R.id.pursuitTypeText)
            val objectivesRecyclerView: RecyclerView = this.itemView.findViewById(R.id.objectivesHolder)
            val pursuitHeader: LinearLayout = this.itemView.findViewById(R.id.pursuitHeader)

            pursuitTitleText.text = pursuit.databaseItem.displayProperties.name
            pursuitDescriptionText.text = pursuit.databaseItem.displayProperties.description
            pursuitTypeText.text = pursuit.databaseItem.itemTypeAndTierDisplayName

            val typeAndTierBreakdown = pursuit.databaseItem.itemTypeAndTierDisplayName.split(" ")
            when(typeAndTierBreakdown[0]) {
                "Common" -> {
                    pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_common_background))
                    pursuitTitleText.setTextColor(Color.parseColor("#000000"))
                    pursuitTypeText.setTextColor(Color.parseColor("#000000"))
                }
                "Rare" -> pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_rare_background))
                "Legendary" -> pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_legendary_background))
                "Exotic" -> pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_exotic_background))
            }

            val layoutManager = LinearLayoutManager(ctx, RecyclerView.VERTICAL, false)
            objectivesRecyclerView.apply {
                this.layoutManager = layoutManager
                val adapter = ObjectivesAdapter()
                this.adapter = adapter
                adapter.setObjectivesList(pursuit.objectives)
                adapter.notifyDataSetChanged()
            }

        }

    }

}