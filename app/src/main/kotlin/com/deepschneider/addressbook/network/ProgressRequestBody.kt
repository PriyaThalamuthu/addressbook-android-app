package com.deepschneider.addressbook.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

interface ProgressCallback {
    fun onProgress(progress: Long)
}

class ProgressRequestBody(
    private val mediaType: MediaType,
    private val file: InputStream,
    private val fileLength: Long,
    private val callback: ProgressCallback
) : RequestBody() {

    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = fileLength

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L
        file.use { fis ->
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                callback.onProgress((100 * uploaded / contentLength()))
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}