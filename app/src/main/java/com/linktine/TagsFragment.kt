package com.linktine

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.linktine.data.types.Tag
import com.linktine.viewmodel.TagViewModel
import com.skydoves.colorpickerview.ColorPickerView

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
        val container = view.findViewById<ChipGroup>(R.id.tagsContainer)
        val fab = view.findViewById<View>(R.id.addTagFab)

        viewModel.tagData.observe(viewLifecycleOwner) { tags ->
            container.removeAllViews()
            tags.forEach { tag ->
                container.addView(createTagView(tag))
            }
        }

        fab.setOnClickListener { showTagDialog() }

        viewModel.loadTags()
    }

    private fun createTagView(tag: Tag): Chip =
        Chip(requireContext()).apply {
            text = tag.name
            chipBackgroundColor = ColorStateList.valueOf(tag.color.toColorInt())
            setTextColor(Color.WHITE)

            setOnLongClickListener {
                showTagDialog(tag)
                true
            }
        }

    private fun showTagDialog(tag: Tag? = null) {
        val context = requireContext()

        // Inflate the dialog layout once
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_color_picker, null, false)
        val input = dialogLayout.findViewById<EditText>(R.id.inputTagName)
        val colorPickerView = dialogLayout.findViewById<ColorPickerView>(R.id.colorPickerView)

        // Pre-fill values if editing
        input.setText(tag?.name)
        var selectedColor = tag?.color?.toColorInt() ?: Color.parseColor("#2196F3")
        colorPickerView.setInitialColor(selectedColor)
        colorPickerView.setPreferenceName("TagColorPicker")
        colorPickerView.setColorListener(object : com.skydoves.colorpickerview.listeners.ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                selectedColor = color
            }
        })

        // Show dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(if (tag == null) "New tag" else "Edit tag")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
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
    }
}