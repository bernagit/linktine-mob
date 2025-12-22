package com.linktine

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.linktine.data.types.Link
import com.linktine.data.types.LinkTag
import com.linktine.data.types.Tag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText

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

        binding.addLinkBtn.setOnClickListener {
            showAddLinkDialog()
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

    private fun showAddLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_link, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputTitle)
        val urlInput = dialogView.findViewById<EditText>(R.id.inputUrl)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add new link")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim().ifEmpty { null }
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    lifecycleScope.launch {
                        linkViewModel.addLink(title, url)
                        triggerReload()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showLinkDetailsDialog(link: Link) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_link_details, null)

        // Editable fields
        val inputTitle = dialogView.findViewById<TextInputEditText>(R.id.inputTitle)
        val inputUrl = dialogView.findViewById<TextInputEditText>(R.id.inputUrl)
        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonEdit)

        // Status toggle buttons
        val statusToggleGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.statusToggleGroup)
        val buttonRead = dialogView.findViewById<MaterialButton>(R.id.buttonRead)
        val buttonArchived = dialogView.findViewById<MaterialButton>(R.id.buttonArchived)
        val buttonFavorite = dialogView.findViewById<MaterialButton>(R.id.buttonFavorite)

        // Other info
        val domain = dialogView.findViewById<TextView>(R.id.detailDomain)
        val description = dialogView.findViewById<TextView>(R.id.detailDescription)
        val created = dialogView.findViewById<TextView>(R.id.detailCreated)
        val updated = dialogView.findViewById<TextView>(R.id.detailUpdated)
        val tagGroup = dialogView.findViewById<ChipGroup>(R.id.detailTagGroup)

        // Populate initial values
        inputTitle.setText(link.title)
        inputUrl.setText(link.url)
        buttonRead.isChecked = link.read
        buttonArchived.isChecked = link.archived
        buttonFavorite.isChecked = link.favorite

        domain.text = "Domain: ${link.domain ?: "-"}"
        description.text = link.description ?: "(No description)"
        created.text = "Created: ${link.createdAt}"
        updated.text = "Updated: ${link.updatedAt}"

        tagGroup.removeAllViews()
        link.tags.forEach { tagGroup.addView(createTagChip(it.tag)) }

        // Disable editing initially
        fun setEditMode(editable: Boolean) {
            inputTitle.isEnabled = editable
            inputUrl.isEnabled = editable

            val alpha = if (editable) 1f else 0.6f
            inputTitle.alpha = alpha
            inputUrl.alpha = alpha
        }

        setEditMode(false)

        buttonEdit.setOnClickListener {
            setEditMode(true)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Link details")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                linkViewModel.updateLink(
                    id = link.id,
                    title = link.title.toString(),
                    url = inputUrl.text.toString(),
                    read = buttonRead.isChecked,
                    archived = buttonArchived.isChecked,
                    favorite = buttonFavorite.isChecked
                )
                triggerReload()
            }
            .setNeutralButton("Delete") { _, _ ->
                confirmDelete(link)
            }
            .setNegativeButton("Close", null)
            .show()

        // Make Delete button red
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
    }

    private fun confirmDelete(link: Link) {
        val confirmDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete link?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteLinkById(link.id)
            }
            .setNegativeButton("Cancel", null)
            .show()

        confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
    }

    private fun deleteLinkById(id: String) {
        lifecycleScope.launch {
            try {
                linkViewModel.deleteLink(id)
                triggerReload()

                Toast.makeText(requireContext(), "Link deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error deleting link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTagChip(tag: Tag): Chip {
        return Chip(requireContext()).apply {
            text = tag.name
            isClickable = false
            isCheckable = false
            setTextColor(ContextCompat.getColor(context, android.R.color.white))

            // Tag color is hex, like "#FF5722"
            val colorInt = tag.color.toColorInt()
            chipBackgroundColor = ColorStateList.valueOf(colorInt)
        }
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
            container.addView(createLinkCard(link))
        }
    }

    private fun createLinkCard(link: Link): View {
        return MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }

            cardElevation = 2f
            radius = 8f
            isClickable = true

            setOnClickListener { openUrl(link.url) }

            setOnLongClickListener {
                showLinkDetailsDialog(link)
                true
            }
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }

            val titleView = TextView(context).apply {
                text = link.title ?: link.name ?: "No title"
                textSize = 16f
            }

            val urlView = TextView(context).apply {
                text = link.url
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

