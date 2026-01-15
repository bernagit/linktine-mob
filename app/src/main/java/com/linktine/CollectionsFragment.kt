package com.linktine

import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.DragEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.linktine.data.types.CollectionsResponse
import com.linktine.databinding.FragmentCollectionsBinding
import com.linktine.ui.collections.CollectionListItem
import com.linktine.ui.collections.CollectionsAdapter
import com.linktine.viewmodel.CollectionsViewModel
import com.skydoves.colorpickerview.ColorPickerView
import kotlinx.coroutines.launch


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

    private var isFabExpanded = false
    private val fabDistance = 170f

    private var draggedItem: CollectionListItem? = null

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
            collectionsViewModel.reloadData()
        }

        collectionsViewModel.collectionsData.observe(viewLifecycleOwner, Observer { data ->
            displayCollectionsData(data)
            binding.homeSwipeRefresh.isRefreshing = false
        })

        collectionsViewModel.currentCollection.observe(viewLifecycleOwner) { current ->
            binding.txtCurrentCollection.text = current?.name ?: "Root"
        }

        collectionsViewModel.reloadData()

        val onBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = collectionsViewModel.goBack()
                if (!handled) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)

        binding.fabMain.setOnClickListener {
            isFabExpanded = !isFabExpanded

            if (isFabExpanded) {
                openFabButtons()
            } else {
                closeFabButtons()
            }
        }

        binding.fabScrim.setOnClickListener {
            closeFabButtons()
        }

        binding.fabAddLink.setOnClickListener { showAddLinkDialog() }
        binding.fabAddCollection.setOnClickListener { showAddCollectionDialog() }

        binding.moveUpCard.setOnDragListener { v, event ->
            actionCardDragListener(v, event, onDrop = { draggedItem ->
                val parentId = collectionsViewModel.currentCollection.value?.parentId
                when(draggedItem) {
                    is CollectionListItem.CollectionItem -> collectionsViewModel.moveCollection(draggedItem.collection.id, parentId)
                    is CollectionListItem.LinkItem -> collectionsViewModel.moveLink(draggedItem.link.id, parentId)
                    null -> {}
                }
            })
        }

        binding.deleteDropCard.setOnDragListener { v, event ->
            actionCardDragListener(v, event, onDrop = { draggedItem ->
                val resourceType = when(draggedItem) {
                    is CollectionListItem.CollectionItem -> "collection"
                    is CollectionListItem.LinkItem -> "link"
                    null -> ""
                }

                showConfirmDialog(
                    message = "Delete this $resourceType?",
                    onConfirm = {
                        when(draggedItem) {
                            is CollectionListItem.CollectionItem -> collectionsViewModel.deleteCollection(draggedItem.collection.id)
                            is CollectionListItem.LinkItem -> collectionsViewModel.deleteLink(draggedItem.link.id)
                            null -> {}
                        }
                    }
                )
            })
        }
    }

    private fun showConfirmDialog(
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm deletion")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun actionCardDragListener(
        v: View,
        event: DragEvent,
        onDrop: (draggedItem: CollectionListItem?) -> Unit
    ): Boolean {
        val materialCard = v as MaterialCardView

        return when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                materialCard.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(150)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
                materialCard.alpha = 0.5f

                true
            }

            DragEvent.ACTION_DRAG_EXITED,
            DragEvent.ACTION_DRAG_ENDED -> {
                materialCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
                materialCard.alpha = 1f

                onDragEnd()
                true
            }

            DragEvent.ACTION_DROP -> {
                val dragged = event.localState as? CollectionListItem
                onDrop(dragged)
                true
            }

            else -> true
        }
    }

    private fun openFabButtons() {
        isFabExpanded = true

        showFab(binding.fabAddCollection, 10f, -fabDistance)
        showFab(binding.fabAddLink, 80f, -fabDistance)

        binding.fabScrim.visibility = View.VISIBLE
    }
    private fun closeFabButtons() {
        isFabExpanded = false

        hideFab(binding.fabAddCollection)
        hideFab(binding.fabAddLink)

        binding.fabScrim.visibility = View.GONE
    }

    private fun showFab(fab: FloatingActionButton, angle: Float, distance: Float) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.scaleX = 0f
        fab.scaleY = 0f

        val rad = Math.toRadians(angle.toDouble())
        val translationX = (distance * Math.cos(rad)).toFloat()
        val translationY = (distance * Math.sin(rad)).toFloat()

        fab.animate()
            .translationX(translationX)
            .translationY(translationY)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }

    private fun hideFab(fab: FloatingActionButton) {
        fab.animate()
            .translationX(0f)
            .translationY(0f)
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(200)
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }

    private fun showAddLinkDialog() {
        closeFabButtons()

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
                        collectionsViewModel.addLink(title, url)
                        collectionsViewModel.reloadData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCollectionDialog() {
        closeFabButtons()

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_collection, null)
        val title = dialogView.findViewById<TextView>(R.id.txtDialogTitle)

        val parentName = collectionsViewModel.currentCollection.value?.name ?: "Root"
        title.text = "Create collection in "
        title.append(
            SpannableString("\"$parentName\"").apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.teal_700)),
                    0, length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0, length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        )

        val nameInput = dialogView.findViewById<EditText>(R.id.inputName)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.inputDescription)
        val colorInput = dialogView.findViewById<ColorPickerView>(R.id.colorPickerView)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val color = "#" + colorInput.color.toHexString().uppercase().substring(2)
                val parentId = collectionsViewModel.currentCollection.value?.id;

                if(!name.isEmpty()) {
                    collectionsViewModel.addCollection(name, description, color, parentId)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun onDragStart() {
        if(collectionsViewModel.currentCollection.value != null) {
            binding.moveUpCard.visibility = View.VISIBLE
        }

        binding.deleteDropCard.visibility = View.VISIBLE
        binding.fabMain.visibility = View.GONE
    }

    private fun onDragEnd() {
        binding.moveUpCard.visibility = View.GONE
        binding.deleteDropCard.visibility = View.GONE
        binding.fabMain.visibility = View.VISIBLE
    }

    private fun displayCollectionsData(data: CollectionsResponse) {
        val uiItems = mutableListOf<CollectionListItem>()

        for (collection in data.collections) {
            uiItems.add(CollectionListItem.CollectionItem(collection))
        }

        for (link in data.links) {
            uiItems.add(CollectionListItem.LinkItem(link))
        }

        val adapter = CollectionsAdapter(
            uiItems,
            onCollectionClick = { selectedCollection ->
                collectionsViewModel.selectCollection(selectedCollection)
            },
            onLinkClick = { link ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = link.url.toUri()
                startActivity(intent)
            },
            onDragStart = { item, view ->
                draggedItem = item

                val clipData = ClipData.newPlainText("item", "")
                val shadow = View.DragShadowBuilder(view)

                view.startDragAndDrop(clipData, shadow, item, 0)
                onDragStart()
            },
            onDropOnCollection = { draggedElement, targetCollectionId ->
                when(draggedElement) {
                    is CollectionListItem.CollectionItem -> {
                        val collectionId = draggedElement.collection.id
                        if(collectionId != targetCollectionId) {
                            collectionsViewModel.moveCollection(collectionId, targetCollectionId)
                        }
                    }
                    is CollectionListItem.LinkItem -> {
                        val linkId = draggedElement.link.id
                        collectionsViewModel.moveLink(linkId, targetCollectionId)
                    }
                }
            },
            onDragEnd = {
                onDragEnd()
            }
        )
        binding.collectionsRecyclerView.adapter = adapter
    }
}