package xyz.omnicron.apps.android.dot.ui.objectives

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.models.DestinyObjectiveData
import xyz.omnicron.apps.android.dot.databinding.ObjectivesListItemBinding

class ObjectivesAdapter: RecyclerView.Adapter<ObjectivesAdapter.ObjectivesHolder>() {

    private var objectives = arrayListOf<DestinyObjectiveData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectivesHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.objectives_list_item, parent, false)
        return ObjectivesHolder(inflatedView)
    }

    override fun getItemCount() = objectives.size

    override fun onBindViewHolder(holder: ObjectivesHolder, position: Int) {
        holder.bindObjective(objectives[position])
    }

    fun setObjectivesList(objectives: ArrayList<DestinyObjectiveData>) {
        this.objectives = objectives
    }

    class ObjectivesHolder(baseView: View): RecyclerView.ViewHolder(baseView), View.OnClickListener {
        private val binding = ObjectivesListItemBinding.bind(baseView)

        override fun onClick(v: View?) {}

        fun bindObjective(objective: DestinyObjectiveData) {
            val descriptionText = "${objective.objectiveDefinition.progressDescription} [${objective.progress} / ${objective.objectiveDefinition.completionValue}]"
            binding.objectiveText.text = descriptionText
            val percentage: Double = (objective.progress.toDouble() / objective.objectiveDefinition.completionValue.toDouble() * 100)
            binding.objectiveProgress.progress = percentage.toInt()
            binding.completionCheckbox.isChecked = objective.progress >= objective.objectiveDefinition.completionValue
        }

    }

}