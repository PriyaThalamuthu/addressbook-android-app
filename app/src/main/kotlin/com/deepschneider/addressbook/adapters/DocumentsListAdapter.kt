package com.deepschneider.addressbook.adapters

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.recyclerview.widget.RecyclerView
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.activities.CreateOrEditPersonActivity
import com.deepschneider.addressbook.databinding.DocumentListItemBinding
import com.deepschneider.addressbook.dto.DocumentDto
import com.deepschneider.addressbook.utils.Constants


class DocumentsListAdapter(
    var documents: MutableList<DocumentDto>,
    private val activity: CreateOrEditPersonActivity
) :
    RecyclerView.Adapter<DocumentsListAdapter.DocumentViewHolder>() {

    var currentAdapterItem: DocumentDto? = null

    inner class DocumentViewHolder(binding: DocumentListItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {
        var currentItem: DocumentDto? = null
        var documentName = binding.name
        var documentDateAndSize = binding.dateAndSize

        init {
            binding.root.setOnCreateContextMenuListener(this)
            binding.root.setOnLongClickListener {
                currentAdapterItem = currentItem
                false
            }
            binding.downloadButton.setOnClickListener {
                currentItem?.url?.let { url ->
                    val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setTitle(currentItem?.name ?: "")
                    request.setDescription(activity.getString(R.string.downloading_in_progress_notification))
                    val map = MimeTypeMap.getSingleton()
                    val ext = MimeTypeMap.getFileExtensionFromUrl(currentItem?.name)
                    request.setMimeType(map.getMimeTypeFromExtension(ext))
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, currentItem?.name ?: ""
                    )
                    dm?.enqueue(request)
                }
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo?) {
            menu.add(
                Menu.NONE,
                Constants.MENU_DELETE_DOCUMENT,
                Menu.NONE,
                activity.getString(R.string.contact_deletion_delete)
            )
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

    override fun getItemId(position: Int): Long {
        return documents[position].hashCode().toLong()
    }
}