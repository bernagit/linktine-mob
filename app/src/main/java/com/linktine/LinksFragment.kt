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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.linktine.data.types.Link
import com.linktine.data.types.Tag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.linktine.viewmodel.TagViewModel
import com.linktine.ui.links.LinksAdapter
import com.linktine.data.types.PaginatedResponse

class LinksFragment : Fragment() {

    private val linkViewModel: LinkViewModel by viewModels {
        LinkViewModel.Factory(requireContext().applicationContext)
    }
    private val tagViewModel: TagViewModel by viewModels {
        TagViewModel.Factory(requireContext().applicationContext)
    }

    // Three-state filter variables
    private var readFilter: Boolean? = null
    private var archivedFilter: Boolean? = null
    private var favoriteFilter: Boolean? = null

    private var _binding: FragmentLinksBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LinksAdapter

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

        binding.linksSwipe.setOnRefreshListener {
            triggerReload()
        }

        binding.addLinkBtn.setOnClickListener {
            showAddLinkDialog()
        }

        adapter = LinksAdapter(
            onClick = { link -> openUrl(link.url) },
            onLongClick = { link -> showLinkDetailsDialog(link) }
        )

        binding.linksRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.linksRecycler.adapter = adapter

        // Observers
        linkViewModel.linkData.observe(viewLifecycleOwner) {
            renderLinks(it)
            binding.linksSwipe.isRefreshing = false
        }

        linkViewModel.error.observe(viewLifecycleOwner) {
            binding.linksSwipe.isRefreshing = false
        }
        tagViewModel.loadTags()

        binding.filterButton.setOnClickListener { view ->
            val popupView = layoutInflater.inflate(R.layout.popup_filters, null)
            val popup = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            popup.elevation = 8f

            val readCheckbox = popupView.findViewById<CheckBox>(R.id.popupFilterRead)
            val archivedCheckbox = popupView.findViewById<CheckBox>(R.id.popupFilterArchived)
            val favoriteCheckbox = popupView.findViewById<CheckBox>(R.id.popupFilterFavorited)

            fun setupThreeState(checkbox: CheckBox, value: Boolean?) {
                when (value) {
                    null -> { checkbox.isChecked = false; checkbox.alpha = 0.5f }
                    true -> { checkbox.isChecked = true; checkbox.alpha = 1f }
                    false -> { checkbox.isChecked = true; checkbox.alpha = 0.5f }
                }
            }

            setupThreeState(readCheckbox, readFilter)
            setupThreeState(archivedCheckbox, archivedFilter)
            setupThreeState(favoriteCheckbox, favoriteFilter)

            // Click cycle through null -> true -> false
            readCheckbox.setOnClickListener {
                readFilter = when (readFilter) {
                    null -> true
                    true -> false
                    false -> null
                }
                setupThreeState(readCheckbox, readFilter)
                triggerReload()
            }

            archivedCheckbox.setOnClickListener {
                archivedFilter = when (archivedFilter) {
                    null -> true
                    true -> false
                    false -> null
                }
                setupThreeState(archivedCheckbox, archivedFilter)
                triggerReload()
            }

            favoriteCheckbox.setOnClickListener {
                favoriteFilter = when (favoriteFilter) {
                    null -> true
                    true -> false
                    false -> null
                }
                setupThreeState(favoriteCheckbox, favoriteFilter)
                triggerReload()
            }

            // Apply filter when popup dismissed
            popup.setOnDismissListener {
                triggerReload(
                    read = readFilter,
                    archived = archivedFilter,
                    favorite = favoriteFilter
                )
            }

            popup.showAsDropDown(view)
        }
        triggerReload()
    }

    private fun triggerReload(
        query: String? = binding.searchQuery.text.toString().trim().ifEmpty { null },
        read: Boolean? = readFilter,
        archived: Boolean? = archivedFilter,
        favorite: Boolean? = favoriteFilter
    ) {
        linkViewModel.loadInitialLinks(
            page = 1,
            limit = 20,
            query = query,
            read = read,
            archived = archived,
            favorite = favorite
        )
    }

    private fun showAddLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_link, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputName)
        val urlInput = dialogView.findViewById<EditText>(R.id.inputUrl)
        val tagInput = dialogView.findViewById<AutoCompleteTextView>(R.id.inputTag)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.tagChipGroup)

        val tags = tagViewModel.tagData.value.orEmpty() // get current tags

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            tags.map { it.name }
        )
        tagInput.setAdapter(adapter)

        // Function to add chip
        fun addTagChip(tagName: String) {
            if (tagName.isBlank()) return

            // Prevent duplicates
            for (i in 0 until chipGroup.childCount) {
                if ((chipGroup.getChildAt(i) as Chip).text == tagName) return
            }

            val existingTag = tags.find { it.name == tagName }

            val chip = Chip(requireContext()).apply {
                text = tagName
                isCloseIconVisible = true
                setOnCloseIconClickListener { chipGroup.removeView(this) }
                if (existingTag != null) chipBackgroundColor = ColorStateList.valueOf(existingTag.color.toColorInt())
            }
            chipGroup.addView(chip)
        }

        // Add chip on dropdown selection
        tagInput.setOnItemClickListener { _, _, position, _ ->
            addTagChip(adapter.getItem(position)!!)
            tagInput.text = null
        }

        // Add chip on enter/done
        tagInput.setOnEditorActionListener { _, _, _ ->
            addTagChip(tagInput.text.toString())
            tagInput.text = null
            true
        }

        // Show dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add new link")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim().ifEmpty { null }
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    val selectedTags = mutableListOf<String>()
                    for (i in 0 until chipGroup.childCount) {
                        val chip = chipGroup.getChildAt(i) as Chip
                        val existingTag = tags.find { it.name == chip.text.toString() }
                        if (existingTag != null) {
                            selectedTags.add(existingTag.name)
                        } else {
                            // Create new tag
                            selectedTags.add(chip.text.toString())
                        }
                    }

                    lifecycleScope.launch {
                        linkViewModel.addLink(title, url, selectedTags)
                        triggerReload()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun setButtonSelected(button: MaterialButton, selected: Boolean, color: Int) {
        if (selected) {
            button.setBackgroundColor(color)
            button.setTextColor(Color.WHITE)
            button.strokeColor = ColorStateList.valueOf(color)
        } else {
            button.setBackgroundColor(Color.TRANSPARENT)
            button.setTextColor(color)
            button.strokeColor = ColorStateList.valueOf(color)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showLinkDetailsDialog(link: Link) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_link_details, null)

        // Editable fields
        val inputName = dialogView.findViewById<TextInputEditText>(R.id.inputName)
        val inputUrl = dialogView.findViewById<TextInputEditText>(R.id.inputUrl)
        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonEdit)

        // Status toggle buttons
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
        setButtonSelected(buttonRead, link.read, ContextCompat.getColor(requireContext(), R.color.teal_300))
        setButtonSelected(buttonArchived, link.archived, ContextCompat.getColor(requireContext(), R.color.gray_300))
        setButtonSelected(buttonFavorite, link.favorite, ContextCompat.getColor(requireContext(), R.color.orange_500))
        inputName.setText(link.name)
        inputUrl.setText(link.url)

        // Initialize button states
        var readSelected = link.read
        var archivedSelected = link.archived
        var favoriteSelected = link.favorite

        setButtonSelected(buttonRead, readSelected, ContextCompat.getColor(requireContext(), R.color.teal_300))
        setButtonSelected(buttonArchived, archivedSelected, ContextCompat.getColor(requireContext(), R.color.gray_300))
        setButtonSelected(buttonFavorite, favoriteSelected, ContextCompat.getColor(requireContext(), R.color.orange_500))

// Click listeners
        buttonRead.setOnClickListener {
            readSelected = !readSelected
            setButtonSelected(buttonRead, readSelected, ContextCompat.getColor(requireContext(), R.color.teal_300))

            linkViewModel.updateLink(
                id = link.id,
                name = inputName.text.toString(),
                url = inputUrl.text.toString(),
                read = readSelected,
                archived = archivedSelected,
                favorite = favoriteSelected
            )
        }

        buttonArchived.setOnClickListener {
            archivedSelected = !archivedSelected
            setButtonSelected(buttonArchived, archivedSelected, ContextCompat.getColor(requireContext(), R.color.gray_300))

            linkViewModel.updateLink(
                id = link.id,
                name = inputName.text.toString(),
                url = inputUrl.text.toString(),
                read = readSelected,
                archived = archivedSelected,
                favorite = favoriteSelected
            )
        }

        buttonFavorite.setOnClickListener {
            favoriteSelected = !favoriteSelected
            setButtonSelected(buttonFavorite, favoriteSelected, ContextCompat.getColor(requireContext(), R.color.orange_500))

            linkViewModel.updateLink(
                id = link.id,
                name = inputName.text.toString(),
                url = inputUrl.text.toString(),
                read = readSelected,
                archived = archivedSelected,
                favorite = favoriteSelected
            )
        }


        domain.text = "Domain: ${link.domain ?: "-"}"
        description.text = link.description ?: "(No description)"
        created.text = "Created: ${link.createdAt}"
        updated.text = "Updated: ${link.updatedAt}"

        tagGroup.removeAllViews()
        link.tags.forEach { tagGroup.addView(createTagChip(it.tag)) }

        // Disable editing initially
        fun setEditMode(editable: Boolean) {
            inputName.isEnabled = editable
            inputUrl.isEnabled = editable

            val alpha = if (editable) 1f else 0.6f
            inputName.alpha = alpha
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
                    name = inputName.text.toString(),
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
            val colorInt = tag.color.toColorInt()
            chipBackgroundColor = ColorStateList.valueOf(colorInt)
        }
    }

    private fun renderLinks(response: PaginatedResponse<Link>) {
        adapter.submit(response.data)
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
                text = when {
                    !link.name.isNullOrBlank() -> link.name
                    !link.title.isNullOrBlank() -> link.title
                    else -> "No title"
                }
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

