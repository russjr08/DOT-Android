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
import org.joda.time.LocalDateTime
import org.joda.time.Period
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.models.DestinyPursuit
import xyz.omnicron.apps.android.dot.inflate
import xyz.omnicron.apps.android.dot.ui.objectives.ObjectivesAdapter
import java.util.*

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

        /**
         * Converts the input to a plural term if quantity == 1, or quantity > 1
         * Example: If the word is 'day' and the quantity is 2, then it will return 'days'
         *          If the quantity is 1, then it will return 'day'
         */
        fun pluralize(quantity: Int, word: String): String {
            if(quantity == 1) {
                return word
            } else {
                return word + 's'
            }
        }

        fun applyExpirationLabel(pursuit: DestinyPursuit, expirationText: TextView) {
            val expirationStrBuilder = StringBuilder()
            val now = LocalDateTime.now()
            val expirationDate = LocalDateTime(pursuit.expirationDate)
            if(now < expirationDate) {
                expirationStrBuilder.append("Expires In: ")
                val diff = Period(now, expirationDate)
                if(diff.days > 0) {
                    expirationStrBuilder.append("${diff.days} ${pluralize(diff.days, "day")} ")
                }
                if(diff.hours > 0) {
                    expirationStrBuilder.append("${diff.hours} ${pluralize(diff.hours, "hour")} ")
                }
                if(diff.minutes > 0) {
                    expirationStrBuilder.append("${diff.minutes} ${pluralize(diff.minutes, "minute")}")
                }

            } else {
                expirationStrBuilder.append("Expired!")
            }

            expirationText.text = expirationStrBuilder.toString()
        }

        fun bindPursuit(pursuit: DestinyPursuit, ctx: Context) {
            // Initialize layout with properties
            val pursuitTitleText: TextView = this.itemView.findViewById(R.id.pursuitTitleText)
            val pursuitDescriptionText: TextView = this.itemView.findViewById(R.id.pursuitDescriptionText)
            val pursuitTypeText: TextView = this.itemView.findViewById(R.id.pursuitTypeText)
            val objectivesRecyclerView: RecyclerView = this.itemView.findViewById(R.id.objectivesHolder)
            val pursuitHeader: LinearLayout = this.itemView.findViewById(R.id.pursuitHeader)
            val expirationText: TextView = this.itemView.findViewById(R.id.expiration_label)

            pursuitTitleText.text = pursuit.databaseItem.displayProperties.name
            pursuitDescriptionText.text = pursuit.databaseItem.displayProperties.description
            pursuitTypeText.text = pursuit.databaseItem.itemTypeAndTierDisplayName

            // Expiration display handling
            if(pursuit.expirationDate != null) {
                applyExpirationLabel(pursuit, expirationText)
                expirationText.visibility = View.VISIBLE
            } else {
                expirationText.visibility = View.GONE
            }

            val typeAndTierBreakdown = pursuit.databaseItem.itemTypeAndTierDisplayName.split(" ")
            when(typeAndTierBreakdown[0]) {
                "Common" -> {
                    pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_common_background))
                    pursuitTitleText.setTextColor(Color.parseColor("#000000"))
                    pursuitTypeText.setTextColor(Color.parseColor("#000000"))
                }
                "Rare" -> {
                    pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_rare_background))
                    pursuitTitleText.setTextColor(Color.parseColor("#FFFFFF"))
                    pursuitTypeText.setTextColor(Color.parseColor("#FFFFFF"))
                }
                "Legendary" -> {
                    pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_legendary_background))
                    pursuitTitleText.setTextColor(Color.parseColor("#FFFFFF"))
                    pursuitTypeText.setTextColor(Color.parseColor("#FFFFFF"))
                }
                "Exotic" -> {
                    pursuitHeader.setBackgroundColor(pursuitHeader.resources.getColor(R.color.pursuit_exotic_background))
                    pursuitTitleText.setTextColor(Color.parseColor("#FFFFFF"))
                    pursuitTypeText.setTextColor(Color.parseColor("#FFFFFF"))
                }
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