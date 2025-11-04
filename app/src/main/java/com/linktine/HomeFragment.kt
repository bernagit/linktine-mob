package com.linktine

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    // Use a custom factory to initialize the ViewModel with the required dependencies (Context)
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(requireContext().applicationContext)
    }

    // View references for the stats and recent items.
    private var statusTextView: TextView? = null
    private var errorIndicator: TextView? = null
    private var totalLinksValue: TextView? = null
    private var readLinksValue: TextView? = null

    // NEW STAT CARDS
    private var favoriteLinksValue: TextView? = null
    private var archivedLinksValue: TextView? = null

    // RECENT LINK CARD
    private var recentLinkTitle: TextView? = null
    private var recentLinkUrl: TextView? = null

    // RECENT COLLECTIONS CONTAINER
    private var recentCollectionsContainer: LinearLayout? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            return inflater.inflate(R.layout.fragment_home, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "FATAL INFLATION ERROR in onCreateView:", e)
            return null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated: Starting view initialization.")

        // Initialize nullable view references.
        statusTextView = view.findViewById(R.id.text_home_status)
        errorIndicator = view.findViewById(R.id.text_error_indicator)

        // Stats Cards
        totalLinksValue = view.findViewById(R.id.stat_total_links_value)
        readLinksValue = view.findViewById(R.id.stat_read_links_value)
        favoriteLinksValue = view.findViewById(R.id.stat_favorite_links_value) // New
        archivedLinksValue = view.findViewById(R.id.stat_archived_links_value) // New

        // Recent Link Card
        recentLinkTitle = view.findViewById(R.id.text_recent_link_title) // New
        recentLinkUrl = view.findViewById(R.id.text_recent_link_url)     // New

        // Collections Container
        recentCollectionsContainer = view.findViewById(R.id.list_recent_collections_container) // New

        // Defensive check for crucial views
        if (recentCollectionsContainer == null) {
            Log.e(TAG, "FATAL LAYOUT ERROR: The new list_recent_collections_container ID was not found.")
        }

        setupObservers()
        homeViewModel.loadInitialData()
    }

    private fun setupObservers() {
        homeViewModel.text.observe(viewLifecycleOwner, Observer { statusText ->
            statusTextView?.text = statusText
            errorIndicator?.visibility = View.GONE
        })

        homeViewModel.dashboardData.observe(viewLifecycleOwner, Observer { data ->
            displayDashboardData(data)
        })

        homeViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            statusTextView?.text = "Error Loading Dashboard"
            errorIndicator?.text = "Error: $errorMessage"
            errorIndicator?.visibility = View.VISIBLE
        })
    }

    @SuppressLint("SetTextI18n")
    private fun displayDashboardData(data: DashboardResponse) {
        // --- Populate Stats Cards (Updated to use new fields) ---
        totalLinksValue?.text = data.stats.totalLinks.toString()
        readLinksValue?.text = data.stats.readLinks.toString()
        favoriteLinksValue?.text = data.stats.favoriteLinks.toString() // New
        archivedLinksValue?.text = data.stats.archivedLinks.toString() // New

        // --- Populate Latest Link Preview ---
        val recentLink = data.recentLinks.firstOrNull()
        if (recentLink != null) {
            recentLinkTitle?.text = recentLink.title
            recentLinkUrl?.text = recentLink.url
        } else {
            recentLinkTitle?.text = "No recent links found."
            recentLinkUrl?.text = "Save your first link to see it here!"
        }

        // --- Populate Recent Collections List (NEW FEATURE) ---
        recentCollectionsContainer?.let { container ->
            container.removeAllViews() // Clear previous views

            // If no recent collections, display a simple message
            if (data.recentCollections.isEmpty()) {
                val noDataText = TextView(context).apply {
                    text = "No recent collections yet."
                    setPadding(16, 16, 16, 16)
                }
                container.addView(noDataText)
            } else {
                // Dynamically create a styled item for each recent collection
                data.recentCollections.take(3).forEach { collection -> // Show max 3 collections
                    container.addView(createCollectionListItem(collection))
                }
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
                    setTextColor(Color.parseColor(collection.color))
                } catch (e: IllegalArgumentException) {
                    // Fallback color if the string is invalid
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
                // Set layout params for margin on the right
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Manually nulling view references
        statusTextView = null
        errorIndicator = null
        totalLinksValue = null
        readLinksValue = null
        favoriteLinksValue = null
        archivedLinksValue = null
        recentLinkTitle = null
        recentLinkUrl = null
        recentCollectionsContainer = null
    }
}
