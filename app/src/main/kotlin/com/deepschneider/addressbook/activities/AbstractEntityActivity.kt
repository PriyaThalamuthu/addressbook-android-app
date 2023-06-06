package com.deepschneider.addressbook.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.dto.AlertDto
import com.deepschneider.addressbook.network.EntityGetRequest
import com.deepschneider.addressbook.utils.NetworkUtils
import com.deepschneider.addressbook.utils.Urls
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class AbstractEntityActivity : AppCompatActivity() {

    protected lateinit var requestQueue: RequestQueue
    protected var serverUrl: String? = null
    protected val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(this)
        serverUrl = NetworkUtils.getServerUrl(this@AbstractEntityActivity)
    }

    protected fun setupOnBackPressedListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentFocus?.clearFocus() ?: run {
                    finish()
                }
            }
        })
    }

    protected fun sendLockRequest(lock: Boolean, cache: String, id: String) {
        val handler = Handler(Looper.getMainLooper())
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val url = "$serverUrl" + (if (lock) Urls.LOCK_RECORD else Urls.UNLOCK_RECORD) + "?type=${cache}" + "&id=${id}"
        executor.execute {
            requestQueue.add(
                EntityGetRequest<AlertDto>(
                    url,
                    {},
                    { error ->
                        handler.post {
                            makeErrorSnackBar(error)
                        }
                    },
                    this@AbstractEntityActivity,
                    object : TypeToken<AlertDto>() {}.type
                ).also { it.tag = getRequestTag() })
        }
    }

    fun makeSnackBar(message: String) {
        val snackBar = Snackbar.make(getParentCoordinatorLayoutForSnackBar(), message, Snackbar.LENGTH_LONG)
        val view: View = snackBar.view
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 10
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        snackBar.show()
    }

    fun makeFileSnackBar(message: String, uri: Uri, mimeType: String?) {
        val snackBar = Snackbar.make(getParentCoordinatorLayoutForSnackBar(), message, Snackbar.LENGTH_LONG)
        snackBar.setAction(this.getString(R.string.document_open_action)) {
            val newIntent = Intent(Intent.ACTION_VIEW)
            newIntent.setDataAndType(uri, mimeType)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(newIntent)
        }
        val view: View = snackBar.view
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 10
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        snackBar.show()
    }

    protected fun makeErrorSnackBar(error: VolleyError) {
        when (error) {
            is AuthFailureError -> makeSnackBar(this.getString(R.string.forbidden_message))
            is TimeoutError -> makeSnackBar(this.getString(R.string.server_timeout_message))
            is ServerError -> {
                val result = error.networkResponse?.data?.toString(Charsets.UTF_8)
                if (result != null) {
                    makeSnackBar(gson.fromJson(result, AlertDto::class.java).headline.toString())
                } else {
                    makeSnackBar(error.message.toString())
                }
            }
            else -> makeSnackBar(error.message.toString())
        }
    }

    abstract fun getParentCoordinatorLayoutForSnackBar(): View

    abstract fun getRequestTag(): String
}