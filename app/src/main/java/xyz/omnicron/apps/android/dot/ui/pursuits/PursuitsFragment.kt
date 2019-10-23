package xyz.omnicron.apps.android.dot.ui.pursuits

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import xyz.omnicron.apps.android.dot.R

class PursuitsFragment : Fragment() {

    companion object {
        fun newInstance() = PursuitsFragment()
    }

    private lateinit var viewModel: PursuitsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pursuits_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PursuitsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
