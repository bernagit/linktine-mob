package com.linktine.ui.profile

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.linktine.R
import com.linktine.data.types.UserProfile

class ProfileAdapter(
    private val list: MutableList<UserProfile>,
    private val onClick: (UserProfile) -> Unit,
    private val onDelete: (UserProfile) -> Unit,
    private var activeProfileId: String = "" // Active profile ID
) : RecyclerView.Adapter<ProfileAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvProfileName)
        val deleteBtn: ImageButton = v.findViewById(R.id.btnDeleteProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_item, parent, false)
        return VH(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val profile = list[position]
        holder.name.text = "${profile.name}@${profile.serverUrl.split('/').last()}"

        // Highlight active profile
        if (profile.id == activeProfileId) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.teal_200)
            )
            holder.name.setTypeface(holder.name.typeface, android.graphics.Typeface.BOLD)
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
            )
            holder.name.setTypeface(holder.name.typeface, android.graphics.Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener { onClick(profile) }
        holder.deleteBtn.setOnClickListener { onDelete(profile) }
    }

    override fun getItemCount() = list.size

    fun remove(profile: UserProfile) {
        val index = list.indexOf(profile)
        if (index != -1) {
            list.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun setActiveProfile(profileId: String) {
        activeProfileId = profileId
        notifyDataSetChanged()
    }
}