package com.linktine

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.linktine.data.SettingsRepository
import com.linktine.data.types.DashboardResponse
import com.linktine.data.types.RecentCollection
import com.linktine.databinding.FragmentHomeBinding
import com.linktine.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(requireContext().applicationContext)
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: SettingsRepository

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SettingsRepository(requireContext().applicationContext)

        // SwipeRefresh triggers ViewModel reload
        binding.homeSwipeRefresh.setOnRefreshListener {
            homeViewModel.loadInitialData()
        }

        setupObservers()

        // Refresh dashboard automatically when active profile changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.activeProfileFlow.collect { profileId ->
                    if (profileId.isNotEmpty()) {
                        homeViewModel.loadInitialData()
                    } else {
                        clearDashboard()
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        // Loading state -> SwipeRefresh
        homeViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.homeSwipeRefresh.isRefreshing = isLoading
        }

        // Dashboard data
        homeViewModel.dashboardData.observe(viewLifecycleOwner) { data ->
            if (data != null) displayDashboardData(data) else clearDashboard()
        }

        // Error
        homeViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                binding.textErrorIndicator.text = "Error: $errorMessage"
                binding.textErrorIndicator.visibility = View.VISIBLE
            } else {
                binding.textErrorIndicator.visibility = View.GONE
            }
        }

        // Text messages
        homeViewModel.text.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                binding.textErrorIndicator.visibility = View.GONE
            }
        }
    }

    private fun displayDashboardData(data: DashboardResponse) {
        binding.statTotalLinksValue.text = data.stats.totalLinks.toString()
        binding.totalCollections.text = data.stats.totalCollections.toString()
        binding.statFavoriteLinksValue.text = data.stats.favoriteLinks.toString()
        binding.statTotalTagsValue.text = data.stats.totalTags.toString()

        // Recent Collections
        binding.listRecentCollectionsContainer.apply {
            removeAllViews()
            if (data.recentCollections.isEmpty()) {
                addView(TextView(context).apply { text = "No recent collections yet."; setPadding(16,16,16,16) })
            } else {
                data.recentCollections.take(3).forEach { addView(createCollectionListItem(it)) }
            }
        }

        // Recent Links
        binding.listRecentLinksContainer.apply {
            removeAllViews()
            if (data.recentLinks.isEmpty()) {
                addView(TextView(context).apply { text = "No recent links yet."; setPadding(16,16,16,16) })
            } else {
                data.recentLinks.take(3).forEach { link ->
                    addView(createRecentLinkCard(
                        title = when {
                            link.name.isNotBlank() -> link.name
                            link.title.isNotBlank() -> link.title
                            else -> "No title"
                        },
                        url = link.url
                    ))
                }
            }
        }
    }

    private fun clearDashboard() {
        binding.statTotalLinksValue.text = "-"
        binding.totalCollections.text = "-"
        binding.statFavoriteLinksValue.text = "-"
        binding.statTotalTagsValue.text = "-"
        binding.listRecentCollectionsContainer.removeAllViews()
        binding.listRecentLinksContainer.removeAllViews()
    }

    private fun createCollectionListItem(collection: RecentCollection): View {
        return MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,4,0,4) }
            cardElevation = 1f
            radius = 8f
            setPadding(16,16,16,16)

            val linearLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16,16,16,16)
            }

            val colorIndicator = TextView(context).apply {
                text = "●"
                textSize = 24f
                try { setTextColor(collection.color.toColorInt()) } catch (_: IllegalArgumentException) {
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 16 }
            }

            val nameTextView = TextView(context).apply {
                text = collection.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            linearLayout.addView(colorIndicator)
            linearLayout.addView(nameTextView)
            addView(linearLayout)
        }
    }

    private fun createRecentLinkCard(title: String, url: String): View {
        return MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,4,0,4) }
            cardElevation = 2f
            radius = 8f
            isClickable = true
            isFocusable = true
            setOnClickListener { openUrl(url) }

            val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16) }

            val titleView = TextView(context).apply { text = title; textSize = 16f; setTypeface(typeface, Typeface.BOLD) }
            val urlView = TextView(context).apply { text = url; textSize = 14f; setTextColor(ContextCompat.getColor(context, R.color.teal_300)) }

            layout.addView(titleView)
            layout.addView(urlView)
            addView(layout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
