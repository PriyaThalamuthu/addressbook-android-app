package com.deepschneider.addressbook.network

interface ProgressCallback {
    fun onProgress(progress: Long)
}