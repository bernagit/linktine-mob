package com.linktine

import TagsAdapter
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.linktine.data.types.Tag
import com.linktine.viewmodel.TagViewModel
import com.skydoves.colorpickerview.ColorPickerView
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener

class TagsFragment : Fragment() {

    private val viewModel: TagViewModel by viewModels {
        TagViewModel.Factory(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tags, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.tagsRecycler)
        val fab = view.findViewById<View>(R.id.addTagFab)

        val adapter = TagsAdapter(
            onClick = { tag ->
                showTagDetailDialog(tag)   // short click
            },
            onLongClick = { tag ->
                showTagEditDialog(tag)     // long click
            }
        )

        recycler.adapter = adapter

        viewModel.tagData.observe(viewLifecycleOwner) {
            adapter.submit(it)
        }

        fab.setOnClickListener {
            showTagEditDialog()   // create new tag
        }

        viewModel.loadTags()
    }

    // ---------------- EDIT DIALOG ----------------

    private fun showTagEditDialog(tag: Tag? = null) {

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_color_picker, null, false)

        val inputName = dialogLayout.findViewById<EditText>(R.id.inputTagName)
        val inputHex = dialogLayout.findViewById<EditText>(R.id.inputHex)
        val preview = dialogLayout.findViewById<View>(R.id.colorPreview)
        val colorPickerView = dialogLayout.findViewById<ColorPickerView>(R.id.colorPickerView)
        var isUpdatingFromPicker = false
        inputName.setText(tag?.name)


        var selectedColor = tag?.color?.toColorInt() ?: "#2196F3".toColorInt()

        fun updateUI(color: Int) {
            selectedColor = color
            preview.setBackgroundColor(color)
            inputHex.setText(String.format("#%06X", 0xFFFFFF and color))
        }

        // initial
        colorPickerView.setInitialColor(selectedColor)
        updateUI(selectedColor)

        // picker -> hex + preview
        colorPickerView.setColorListener(object : com.skydoves.colorpickerview.listeners.ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                selectedColor = color

                isUpdatingFromPicker = true
                updateUI(color)
                isUpdatingFromPicker = false
            }
        })


        // hex -> picker + preview
        inputHex.addTextChangedListener {
            if (isUpdatingFromPicker) return@addTextChangedListener

            val text = it.toString()
            if (text.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                val color = text.toColorInt()
                selectedColor = color
                preview.setBackgroundColor(color)
                colorPickerView.setInitialColor(color)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (tag == null) "New tag" else "Edit tag")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton

                val hexColor = String.format("#%06X", 0xFFFFFF and selectedColor)

                if (tag == null)
                    viewModel.createTag(name, hexColor)
                else
                    viewModel.updateTag(tag.id, name, hexColor)
            }
            .setNegativeButton("Cancel", null)
            .apply {
                if (tag != null) {
                    setNeutralButton("Delete") { _, _ ->
                        viewModel.deleteTag(tag.id)
                    }
                }
            }
            .show()

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
    }

    // ---------------- DETAIL DIALOG ----------------

    private fun showTagDetailDialog(tag: Tag) {

        val view = layoutInflater.inflate(R.layout.dialog_tag_detail, null)

        val name = view.findViewById<TextView>(R.id.tagName)
        val dot = view.findViewById<View>(R.id.colorDot)
        val date = view.findViewById<TextView>(R.id.tagDate)
        val count = view.findViewById<TextView>(R.id.tagCount)
        val linksContainer = view.findViewById<LinearLayout>(R.id.linksContainer)

        name.text = tag.name
        dot.background.setTint(tag.color.toColorInt())
        date.text = "Created: ${tag.createdAt}"
        count.text = "Links: ${tag.links.size}"

        linksContainer.removeAllViews()

        tag.links.forEach { tagLink ->
            val link = tagLink.link

            val tv = TextView(requireContext()).apply {
                text = when {
                    !link.name.isNullOrBlank() -> link.name
                    !link.title.isNullOrBlank() -> link.title
                    else -> "No title"
                }
                textSize = 15f
                setPadding(0, 8, 0, 8)
                setTextColor(requireContext().getColor(R.color.purple_300))
                paint.isUnderlineText = true
            }

            tv.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                startActivity(intent)
            }

            linksContainer.addView(tv)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setPositiveButton("Edit") { _, _ ->
                showTagEditDialog(tag)
            }
            .setNeutralButton("Delete") { _, _ ->
                viewModel.deleteTag(tag.id)
            }
            .setNegativeButton("Close", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
    }
}
