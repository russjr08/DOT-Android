package xyz.omnicron.apps.android.dot.ui.objectives

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.database.DestinyDatabaseItem
import xyz.omnicron.apps.android.dot.databinding.RewardsListItemBinding

class RewardsAdapter: RecyclerView.Adapter<RewardsAdapter.RewardsHolder>() {

    private var rewards = arrayListOf<DestinyDatabaseItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardsHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.rewards_list_item, parent, false)
        return RewardsHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: RewardsHolder, position: Int) {
        holder.bindReward(rewards[position], holder.itemView.context)
    }

    override fun getItemCount(): Int = rewards.size

    fun setRewards(rewards: ArrayList<DestinyDatabaseItem>) {
        this.rewards = rewards
    }

    class RewardsHolder(private val baseView: View): RecyclerView.ViewHolder(baseView) {

        private val binding = RewardsListItemBinding.bind(baseView)

        fun bindReward(reward: DestinyDatabaseItem, ctx: Context) {
            binding.rewardDescription.text = reward.displayProperties?.name
            Picasso.with(ctx).load("https://bungie.net${reward.displayProperties!!.icon}").into(binding.rewardImage)
        }

    }
}