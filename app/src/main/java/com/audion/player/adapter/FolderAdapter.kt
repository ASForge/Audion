package com.audion.player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audion.player.databinding.ItemFolderBinding
import com.audion.player.model.MusicFolder

class FolderAdapter(
    private val onFolderClick: (MusicFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private val items = mutableListOf<MusicFolder>()

    fun submitList(newList: List<MusicFolder>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class FolderViewHolder(private val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: MusicFolder) {
            binding.tvFolderName.text = folder.folderName
            binding.tvFolderMeta.text = "${folder.trackCount} lagu • ${folder.storageType} (${folder.folderPath})"

            binding.root.contentDescription =
                "Folder ${folder.folderName}, ${folder.trackCount} lagu, di ${folder.storageType}, path ${folder.folderPath}. Ketuk dua kali untuk memutar folder ini."

            binding.root.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}