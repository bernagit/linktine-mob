import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.linktine.R
import com.linktine.data.types.Tag

class TagsAdapter(
    private val onClick: (Tag) -> Unit,
    private val onLongClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagsAdapter.TagVH>() {

    private val items = mutableListOf<Tag>()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Tag>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class TagVH(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tagName)
        val dot = view.findViewById<View>(R.id.colorDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.tag_item, parent, false)
        return TagVH(v)
    }

    override fun onBindViewHolder(holder: TagVH, position: Int) {
        val tag = items[position]

        holder.name.text = tag.name
        holder.dot.background.setTint(tag.color.toColorInt())

        holder.itemView.setOnClickListener {
            onClick(tag)          // SHORT CLICK
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(tag)     // LONG CLICK
            true
        }
    }

    override fun getItemCount() = items.size
}
