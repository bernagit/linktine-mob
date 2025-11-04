package com.linktine

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.card.MaterialCardView
import com.linktine.data.DashboardResponse
import com.linktine.data.RecentCollection
import com.linktine.viewmodel.HomeViewModel
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class HomeFragment : Fragment() {
    // Use a custom factory to initialize the ViewModel with the required dependencies (Context)
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(requireContext().applicationContext)
    }

    // View references for the stats and recent items.
    private var errorIndicator: TextView? = null
    private var totalLinksValue: TextView? = null
    private var totalCollectionsValue: TextView? = null
    private var favoriteLinksValue: TextView? = null
    private var tagsValue: TextView? = null
    private var recentLinksContainer: LinearLayout? = null
    private var swipeRefresh: SwipeRefreshLayout? = null

    // RECENT COLLECTIONS CONTAINER
    private var recentCollectionsContainer: LinearLayout? = null

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_home, container, false)
        } catch (e: Exception) {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize nullable view references.
        errorIndicator = view.findViewById(R.id.text_error_indicator)

        // Stats Cards
        totalLinksValue = view.findViewById(R.id.stat_total_links_value)
        totalCollectionsValue = view.findViewById(R.id.totalCollections)
        favoriteLinksValue = view.findViewById(R.id.stat_favorite_links_value)
        tagsValue = view.findViewById(R.id.stat_total_tags_value)

        // Collections Container
        recentCollectionsContainer = view.findViewById(R.id.list_recent_collections_container)

        // Recent Links Container
        recentLinksContainer = view.findViewById(R.id.list_recent_links_container)

        // refresh
        swipeRefresh = view.findViewById(R.id.home_swipe_refresh)
        swipeRefresh?.setOnRefreshListener {
            swipeRefresh!!.isRefreshing = true;
            homeViewModel.loadInitialData() // reload
            swipeRefresh!!.isRefreshing = false;
        }

        setupObservers()
        homeViewModel.loadInitialData()
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        homeViewModel.text.observe(viewLifecycleOwner, Observer { statusText ->
            errorIndicator?.visibility = View.GONE
        })

        homeViewModel.dashboardData.observe(viewLifecycleOwner, Observer { data ->
            displayDashboardData(data)
        })

        homeViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorIndicator?.text = "Error: $errorMessage"
            errorIndicator?.visibility = View.VISIBLE
        })
    }

    @SuppressLint("SetTextI18n")
    private fun displayDashboardData(data: DashboardResponse) {
        totalLinksValue?.text = data.stats.totalLinks.toString()
        totalCollectionsValue?.text = data.stats.totalCollections.toString()
        favoriteLinksValue?.text = data.stats.favoriteLinks.toString()
        tagsValue?.text = data.stats.totalTags.toString()

        recentCollectionsContainer?.let { container ->
            container.removeAllViews()
            if (data.recentCollections.isEmpty()) {
                val noDataText = TextView(context).apply {
                    text = "No recent collections yet."
                    setPadding(16, 16, 16, 16)
                }
                container.addView(noDataText)
            } else {
                data.recentCollections.take(3).forEach { collection ->
                    container.addView(createCollectionListItem(collection))
                }
            }
        }

        recentLinksContainer?.let { container ->
            container.removeAllViews()

            if (data.recentLinks.isEmpty()) {
                val none = TextView(context).apply {
                    text = "No recent links yet."
                    setPadding(16, 16, 16, 16)
                }
                container.addView(none)
                return@let
            }

            data.recentLinks.take(3).forEach { link ->
                container.addView(createRecentLinkCard(link.title, link.url))
            }
        }


        // Hide the error indicator if data successfully loaded
        errorIndicator?.visibility = View.GONE
    }

    // Helper function to dynamically create a view for each collection
    private fun createCollectionListItem(collection: RecentCollection): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4) // Spacing between items
            }
            // Basic Material Components styling
            cardElevation = 1f
            radius = 8f
            setPadding(16, 16, 16, 16)

            // Set up the content view
            val linearLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }

            // 1. Color Indicator (Circle)
            val colorIndicator = TextView(context).apply {
                text = "●"
                textSize = 24f
                try {
                    // Try to parse the color string (e.g., "#FF0000")
                    setTextColor(collection.color.toColorInt())
                } catch (_: IllegalArgumentException) {
                    // Fallback color if the string is invalid
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                // Set layout params for margin on the right
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 16 }
            }

            // 2. Collection Name
            val nameTextView = TextView(context).apply {
                text = collection.name
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Take up remaining space
                )
            }

            linearLayout.addView(colorIndicator)
            linearLayout.addView(nameTextView)

            addView(linearLayout)
        }
        return card
    }

    private fun createRecentLinkCard(title: String, url: String): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }

            cardElevation = 2f
            radius = 8f
            isClickable = true
            isFocusable = true

            setOnClickListener {
                openUrl(url)
            }

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            }

            val urlView = TextView(context).apply {
                text = url
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.teal_700))
            }

            layout.addView(titleView)
            layout.addView(urlView)

            addView(layout)
        }

        return card
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Manually nulling view references
        errorIndicator = null
        totalLinksValue = null
        totalCollectionsValue = null
        favoriteLinksValue = null
        tagsValue = null
        recentCollectionsContainer = null
    }
}
