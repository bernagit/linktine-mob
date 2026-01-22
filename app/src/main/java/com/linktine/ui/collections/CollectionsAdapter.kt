package com.linktine.ui.collections

import android.content.Intent
import android.graphics.Color
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.linktine.R
import com.linktine.data.types.Collection
import com.linktine.data.types.Link

// Sealed class already defined
sealed class CollectionListItem {

    data class CollectionItem(val collection: Collection) : CollectionListItem()
    data class LinkItem(val link: Link) : CollectionListItem()
}

class CollectionsAdapter(
    private val items: List<CollectionListItem>,
    private val onCollectionClick: (Collection) -> Unit,
    private val onLinkClick: (Link) -> Unit,
    private val onDragStart: (CollectionListItem, View) -> Unit,
    private val onDropOnCollection: (draggedElement: CollectionListItem, targetCollectionId: String) -> Unit,
    private val onDragEnd: () -> Unit,
    private val showEditCollectionDialog: (collection: Collection) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_COLLECTION = 0
        private const val TYPE_LINK = 1
    }

    class CollectionViewHolder(
        view: View,
        private val onClick: (Collection) -> Unit,
        private val showEditCollectionDialog: (collection: Collection) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        val name: TextView = view.findViewById(R.id.txtCollectionName)
        val description: TextView = view.findViewById(R.id.txtCollectionDescription)
        val colorIndicator: View = view.findViewById(R.id.colorIndicator)
        val imageButton: ImageButton = view.findViewById(R.id.btnEdit)

        fun bind(collection: Collection) {
            name.text = collection.name
            description.text = collection.description

            try {
                val colorInt = collection.color.toColorInt()
                colorIndicator.setBackgroundColor(colorInt)
            } catch (e: Exception) {
                colorIndicator.setBackgroundColor(Color.LTGRAY)
            }

            itemView.setOnClickListener { onClick(collection) }
            imageButton.setOnClickListener { showEditCollectionDialog(collection) }
        }
    }

    class LinkViewHolder(view: View, private val onClick: (Link) -> Unit) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtLinkTitle)
        val domain: TextView = view.findViewById(R.id.txtLinkDomain)
        private val thumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)

        fun bind(link: Link) {
            title.text = when(link.name) {
                "", null -> link.title
                else -> link.name
            }
            domain.text = link.domain

            val placeholder = R.drawable.ic_link

            Glide.with(thumbnail.context)
                .load(link.thumbnail)
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .into(thumbnail)

            itemView.setOnClickListener {
                onClick(link)
            }

            btnShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link.url)
                }

                val chooser = Intent.createChooser(intent, "Share link via")
                it.context.startActivity(chooser)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CollectionListItem.CollectionItem -> TYPE_COLLECTION
            is CollectionListItem.LinkItem -> TYPE_LINK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_COLLECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.collection_item, parent, false)
                CollectionViewHolder(view, onCollectionClick, showEditCollectionDialog)
            }
            TYPE_LINK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.link_item, parent, false)
                LinkViewHolder(view, onLinkClick)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is CollectionViewHolder -> if (item is CollectionListItem.CollectionItem) {
                holder.bind(item.collection)

                holder.itemView.setOnDragListener { _, event ->
                    when (event.action) {

                        DragEvent.ACTION_DRAG_STARTED -> {
                            // IMPORTANT: must return true to receive further events
                            true
                        }

                        DragEvent.ACTION_DRAG_ENTERED -> {
                            holder.itemView.alpha = 0.5f
                            true
                        }

                        DragEvent.ACTION_DRAG_EXITED -> {
                            holder.itemView.alpha = 1f
                            true
                        }

                        DragEvent.ACTION_DROP -> {
                            val dragged = event.localState as? CollectionListItem
                            if(dragged != null) {
                                onDropOnCollection(dragged, item.collection.id)
                            }
                            true
                        }

                        DragEvent.ACTION_DRAG_ENDED -> {
                            holder.itemView.alpha = 1f
                            onDragEnd()
                            true
                        }

                        else -> false
                    }
                }
            }

            is LinkViewHolder -> if (item is CollectionListItem.LinkItem)
                holder.bind(item.link)
        }

        holder.itemView.setOnLongClickListener {
            onDragStart(item, it)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
