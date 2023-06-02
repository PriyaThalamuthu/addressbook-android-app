package com.deepschneider.addressbook.utils

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.preference.PreferenceManager
import com.deepschneider.addressbook.dto.FilterDto
import java.io.Serializable

object Utils {
    fun getTextFilterDto(name: String, value: String?): FilterDto? {
        if (value.isNullOrBlank()) return null
        val filterDto = FilterDto()
        filterDto.name = name
        filterDto.value = value
        filterDto.comparator = ""
        filterDto.type = "TextFilter"
        return filterDto
    }

    fun getDateFilterDto(name: String, value: String?, comparator: String?): FilterDto? {
        if (value.isNullOrBlank() || comparator.isNullOrBlank()) return null
        val filterDto = FilterDto()
        filterDto.name = name
        filterDto.value = value
        filterDto.comparator = comparator
        filterDto.type = "DateFilter"
        return filterDto
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    fun <T : Serializable?> getSerializable(activity: Activity, name: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            activity.intent.getSerializableExtra(name, clazz)
        else
            activity.intent.getSerializableExtra(name) as T?
    }

    fun saveBiometrics(context: Context, data: ByteArray) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Constants.BIOMETRICS, Base64.encodeToString(data, Base64.NO_WRAP))
            .commit()
    }

    fun getBiometrics(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.BIOMETRICS, null)
    }

    fun getFileName(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor.use { cur ->
                if (cur != null && cur.moveToFirst()) {
                    val idx = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(idx != null && idx > 0) {
                        result = cur.getString(idx)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun getFileSize(context: Context, uri: Uri?): Long? {
        if (uri == null) return null
        var result: Long? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor.use { cur ->
                if (cur != null && cur.moveToFirst()) {
                    val idx = cursor?.getColumnIndex(OpenableColumns.SIZE)
                    if(idx != null && idx > 0) {
                        result = cur.getLong(idx)
                    }
                }
            }
        }
        return result
    }
}