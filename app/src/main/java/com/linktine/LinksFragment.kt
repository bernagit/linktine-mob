package com.linktine

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.linktine.databinding.FragmentLinksBinding
import com.linktine.viewmodel.LinkViewModel
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch


class LinksFragment : Fragment() {

    private val linkViewModel: LinkViewModel by viewModels {
        LinkViewModel.Factory(requireContext().applicationContext)
    }

    private var _binding: FragmentLinksBinding? = null
    private val binding get() = _binding!!

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(FlowPreview::class)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            binding.searchQuery.textChanges()
                .debounce(300)
                .map { it.toString().trim().ifEmpty { null } }
                .distinctUntilChanged()
                .collect { query ->
                    triggerReload(query)
                }
        }
        binding.searchQuery.setOnEditorActionListener { view, _, _ ->
            val query = view.text.toString().trim().ifEmpty { null }
            triggerReload(query)
            true
        }

        binding.filterRead.setOnCheckedChangeListener { _, isChecked ->
            triggerReload(read = isChecked)
        }

        binding.filterArchived.setOnCheckedChangeListener { _, isChecked ->
            triggerReload(archived = isChecked)
        }

        binding.linksSwipe.setOnRefreshListener {
            triggerReload()
        }

        // Observers
        linkViewModel.linkData.observe(viewLifecycleOwner) {
            renderLinks(it)
            binding.linksSwipe.isRefreshing = false
        }

        linkViewModel.error.observe(viewLifecycleOwner) {
            renderError(it)
            binding.linksSwipe.isRefreshing = false
        }

        // Initial load
        triggerReload()
    }

    private fun triggerReload(
        query: String? = binding.searchQuery.text.toString().trim().ifEmpty { null },
        read: Boolean? = if (binding.filterRead.isChecked) true else null,
        archived: Boolean? = if (binding.filterArchived.isChecked) true else null,
    ) {
        linkViewModel.loadInitialLinks(
            page = 1,
            limit = 20,
            query = query,
            read = read,
            archived = archived
        )
    }

    @SuppressLint("SetTextI18n")
    private fun renderError(msg: String) {
        val container = binding.linksContainer
        container.removeAllViews()
        container.addView(
            TextView(requireContext()).apply {
                text = "Error: $msg"
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                textSize = 16f
            }
        )
    }

    private fun renderLinks(response: com.linktine.data.types.PaginatedResponse<com.linktine.data.types.Link>) {
        val container = binding.linksContainer
        container.removeAllViews()

        if (response.data.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "No links yet."
                textSize = 16f
            })
            return
        }

        response.data.forEach { link ->
            container.addView(createLinkCard(link.title, link.url))
        }
    }

    private fun createLinkCard(title: String?, url: String): View {
        return MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }

            cardElevation = 2f
            radius = 8f
            isClickable = true
            setOnClickListener { openUrl(url) }

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 16f
            }

            val urlView = TextView(context).apply {
                text = url
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.teal_300))
            }

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

fun EditText.textChanges(): Flow<CharSequence?> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            trySend(s)
        }
        override fun afterTextChanged(s: Editable?) {}
    }
    addTextChangedListener(watcher)
    awaitClose { removeTextChangedListener(watcher) }
}

