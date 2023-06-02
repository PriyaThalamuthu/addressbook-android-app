package com.deepschneider.addressbook.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.deepschneider.addressbook.BuildConfig
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.activities.CreateOrEditPersonActivity
import java.io.File

class DownloadBroadcastReceiver(private val activity: CreateOrEditPersonActivity) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadManager =
            activity.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val dmQuery = DownloadManager.Query()
            dmQuery.setFilterById(referenceId)
            try {
                downloadManager.query(dmQuery).use { cursor ->
                    if (cursor != null && cursor.count > 0) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val columnTitle = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        if (cursor.moveToFirst() && cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            val columnUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val map = MimeTypeMap.getSingleton()
                            val mimeType = map.getMimeTypeFromExtension(
                                MimeTypeMap.getFileExtensionFromUrl(cursor.getString(columnTitle))
                            )
                            Uri.parse(cursor.getString(columnUri)).path?.let { path ->
                                activity.makeFileSnackBar(
                                    cursor.getString(columnTitle) + activity.getString(R.string.downloading_finished_message),
                                    FileProvider.getUriForFile(
                                        activity,
                                        BuildConfig.APPLICATION_ID + ".provider",
                                        File(path)
                                    ),
                                    mimeType
                                )
                            }
                        } else {
                            activity.makeSnackBar(
                                cursor.getString(columnTitle) + activity.getString(R.string.downloading_failed_message)
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                activity.makeSnackBar(activity.getString(R.string.downloading_failed_message))
            }
        }

        if (DownloadManager.ACTION_NOTIFICATION_CLICKED == intent.action) {
            val referenceId =
                intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
            if (referenceId != null) {
                if (referenceId.isNotEmpty()) {
                    val dmQuery = DownloadManager.Query()
                    dmQuery.setFilterById(*referenceId)
                    try {
                        downloadManager.query(dmQuery).use { cursor ->
                            if (cursor != null && cursor.count > 0) {
                                val columnIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val columnTitle =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                                if (cursor.moveToFirst() && cursor.getInt(columnIndex) == DownloadManager.STATUS_RUNNING) {
                                    activity.makeSnackBar(
                                        cursor.getString(columnTitle) + activity.getString(R.string.downloading_in_progress_message)
                                    )
                                } else {
                                    activity.makeSnackBar(
                                        cursor.getString(columnTitle) + activity.getString(R.string.downloading_failed_message)
                                    )
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        activity.makeSnackBar(activity.getString(R.string.downloading_failed_message))
                    }
                }
            }
        }
    }
}