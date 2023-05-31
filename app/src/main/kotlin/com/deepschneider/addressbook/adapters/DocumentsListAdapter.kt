package com.deepschneider.addressbook.adapters

import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.deepschneider.addressbook.databinding.DocumentListItemBinding
import com.deepschneider.addressbook.dto.DocumentDto


class DocumentsListAdapter(
    var documents: List<DocumentDto>
) :
    RecyclerView.Adapter<DocumentsListAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(binding: DocumentListItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {
        var currentItem: DocumentDto? = null
        var documentName = binding.name
        var documentDateAndSize = binding.dateAndSize

        init {
            binding.root.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo?) {
            v?.id?.let {
                menu.add(Menu.NONE, it, Menu.NONE, "Download")
                menu.add(Menu.NONE, it, Menu.NONE, "Delete")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        return DocumentViewHolder(
            DocumentListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.currentItem = documents[position]
        holder.documentName.text = documents[position].name
        holder.documentDateAndSize.text =
            documents[position].createDate + " | " + documents[position].size
    }

    override fun getItemCount(): Int {
        return documents.size
    }
}