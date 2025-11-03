package com.linktine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Use the KTX delegate
import androidx.lifecycle.Observer
import com.linktine.R // Import your resources R file
import com.linktine.viewmodel.HomeViewModel

class HomeFragment : Fragment() {

    // Use the KTX delegate for easy ViewModel instantiation
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = view.findViewById(R.id.text_home)

        // --- View observes ViewModel LiveData ---
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        // Trigger data loading when the view is created
        homeViewModel.loadInitialData()
    }
}