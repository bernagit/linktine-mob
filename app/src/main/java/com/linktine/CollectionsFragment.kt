package com.linktine

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.linktine.data.CollectionsResponse
import com.linktine.databinding.FragmentCollectionsBinding
import com.linktine.databinding.FragmentHomeBinding
import com.linktine.ui.collections.CollectionsAdapter
import com.linktine.viewmodel.CollectionsViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [CollectionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CollectionsFragment : Fragment() {
    private val collectionsViewModel: CollectionsViewModel by viewModels {
        CollectionsViewModel.Factory(requireContext().applicationContext)
    }

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collectionsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.homeSwipeRefresh.setOnRefreshListener {
            collectionsViewModel.loadInitialData()
        }

        setupObservers()
        collectionsViewModel.loadInitialData()
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {

//        collectionsViewModel.text.observe(viewLifecycleOwner, Observer {
//            binding.textErrorIndicator.visibility = View.GONE
//        })

        collectionsViewModel.collectionsData.observe(viewLifecycleOwner, Observer { data ->
            displayCollectionsData(data)
            binding.homeSwipeRefresh.isRefreshing = false
        })

//        collectionsViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
//            binding.textErrorIndicator.text = "Error: $errorMessage"
//            binding.textErrorIndicator.visibility = View.VISIBLE
//            binding.homeSwipeRefresh.isRefreshing = false
//        })
    }

    private fun displayCollectionsData(data: CollectionsResponse) {
        val adapter = CollectionsAdapter(data.data)
        binding.collectionsRecyclerView.adapter = adapter
    }
}