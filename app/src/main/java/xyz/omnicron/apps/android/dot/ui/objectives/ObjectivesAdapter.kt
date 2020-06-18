package xyz.omnicron.apps.android.dot.ui.objectives

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import xyz.omnicron.apps.android.dot.R
import xyz.omnicron.apps.android.dot.api.models.DestinyObjectiveData

class ObjectivesAdapter: RecyclerView.Adapter<ObjectivesAdapter.ObjectivesHolder>() {

    private var objectives = arrayListOf<DestinyObjectiveData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectivesHolder {
        //val inflatedView = parent.inflate(R.layout.objectives_item, false)
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.objectives_item, parent, false)
        return ObjectivesHolder(inflatedView)
    }

    override fun getItemCount() = objectives.size

    override fun onBindViewHolder(holder: ObjectivesHolder, position: Int) {
        holder.bindObjective(objectives[position])
    }

    fun setObjectivesList(objectives: ArrayList<DestinyObjectiveData>) {
        this.objectives = objectives
    }

    class ObjectivesHolder(var baseView: View): RecyclerView.ViewHolder(baseView), View.OnClickListener {
        private var objectiveTitleText: TextView = this.itemView.findViewById(R.id.objective_text)
        private val objectiveProgressBar: ProgressBar = this.itemView.findViewById(R.id.objective_progress)
        private val objectiveCheckBox: CheckBox = this.itemView.findViewById(R.id.completion_checkbox)

        override fun onClick(v: View?) {}

        fun bindObjective(objective: DestinyObjectiveData) {
            objectiveTitleText.text = objective.objectiveDefinition.progressDescription
            objectiveProgressBar.progress = objective.progress / objective.objectiveDefinition.completionValue
            objectiveCheckBox.isChecked = objective.progress >= objective.objectiveDefinition.completionValue
        }

    }

}