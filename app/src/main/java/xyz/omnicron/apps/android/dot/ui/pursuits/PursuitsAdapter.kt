package xyz.omnicron.apps.android.dot.ui.pursuits

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.models.DestinyPursuit
import xyz.omnicron.apps.android.dot.inflate

class PursuitsAdapter: RecyclerView.Adapter<PursuitsAdapter.PursuitHolder>() {

    private var pursuits = arrayListOf<DestinyPursuit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PursuitsAdapter.PursuitHolder {
        val inflatedView = parent.inflate(R.layout.pursuit_card, false)
        return PursuitHolder(inflatedView)
    }

    override fun getItemCount() = pursuits.size

    override fun onBindViewHolder(holder: PursuitsAdapter.PursuitHolder, position: Int) {
        val pursuit = pursuits[position]
        holder.bindPursuit(pursuit)
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

        fun bindPursuit(pursuit: DestinyPursuit) {
            // Initialize layout with properties
            val pursuitTitleText: TextView = this.itemView.findViewById(R.id.pursuitTitleText)
            val pursuitDescriptionText: TextView = this.itemView.findViewById(R.id.pursuitDescriptionText)
            val pursuitTypeText: TextView = this.itemView.findViewById(R.id.pursuitTypeText)
            pursuitTitleText.text = pursuit.databaseItem.displayProperties.name
            pursuitDescriptionText.text = pursuit.databaseItem.displayProperties.description
            pursuitTypeText.text = pursuit.databaseItem.itemTypeAndTierDisplayName
        }

    }

}