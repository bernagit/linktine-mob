package com.linktine.ui.links

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.linktine.R
import com.linktine.data.types.Link

class LinksAdapter(
    private val onClick: (Link) -> Unit,
    private val onLongClick: (Link) -> Unit
) : RecyclerView.Adapter<LinksAdapter.VH>() {

    private val items = mutableListOf<Link>()

    fun submit(list: List<Link>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtLinkTitle)
        val domain: TextView = view.findViewById(R.id.txtLinkDomain)
        val thumbnail: ImageView = view.findViewById(R.id.imgThumbnail)

        val share: ImageView = view.findViewById(R.id.btnShare)
    }

    private fun shareLink(context: android.content.Context, link: Link) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, link.url)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share link via")
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.link_item, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val link = items[pos]

        h.title.text = link.name ?: link.title ?: "No title"
        h.domain.text = link.domain ?: link.url

        h.itemView.setOnClickListener { onClick(link) }
        h.itemView.setOnLongClickListener {
            onLongClick(link)
            true
        }

        h.share.setOnClickListener {
            shareLink(h.itemView.context, link)
        }

        val placeholder = R.drawable.ic_link

        Glide.with(h.thumbnail.context)
            .load(link.thumbnail)
            .centerCrop()
            .placeholder(placeholder)
            .error(placeholder)
            .into(h.thumbnail)
    }
}
