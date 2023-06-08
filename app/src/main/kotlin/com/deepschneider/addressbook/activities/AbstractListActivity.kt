package com.deepschneider.addressbook.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.drawer.Drawer
import com.deepschneider.addressbook.dto.AlertDto
import com.deepschneider.addressbook.dto.FilterDto
import com.deepschneider.addressbook.network.FilteredListRequest
import com.deepschneider.addressbook.network.LogoutRequest
import com.deepschneider.addressbook.utils.Constants
import com.deepschneider.addressbook.utils.NetworkUtils
import com.deepschneider.addressbook.utils.Urls
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class AbstractListActivity<in T> : AppCompatActivity() {

    protected lateinit var toggle: ActionBarDrawerToggle
    protected lateinit var requestQueue: RequestQueue
    protected lateinit var mainDrawer: Drawer
    protected var serverUrl: String? = null
    private val gson = Gson()
    protected var totalListSize: Int? = null
    protected var sortName: String = "id"
    protected var sortOrder: String = "asc"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(this)
        serverUrl = NetworkUtils.getServerUrl(this@AbstractListActivity)
    }

    protected fun prepareActionBar(drawer: Drawer) {
        mainDrawer = drawer
        mainDrawer.setInterceptTouchEventChildId(R.id.roles_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toggle = ActionBarDrawerToggle(this, mainDrawer, R.string.drawer_opened, R.string.drawer_closed)
        mainDrawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    abstract fun getRequestTag(): String

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(getRequestTag())
    }

    protected fun makeErrorSnackBar(error: VolleyError) {
        Snackbar.make(
            getParentCoordinatorLayoutForSnackBar(), when (error) {
                is AuthFailureError -> this.getString(R.string.forbidden_message)
                is TimeoutError -> this.getString(R.string.server_timeout_message)
                is ServerError -> {
                    val result = error.networkResponse?.data?.toString(Charsets.UTF_8)
                    if (result != null) {
                        gson.fromJson(result, AlertDto::class.java).headline.toString()
                    } else {
                        error.message.toString()
                    }
                }
                else -> error.message.toString()
            }, Snackbar.LENGTH_LONG
        ).show()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun updateList(filterDto: List<FilterDto>) {
        getMainList().visibility = View.GONE
        getTotalListSizeTextView().visibility = View.GONE
        getEmptyListView().visibility = View.GONE
        val progressBar = getProgressBar()
        progressBar.visibility = ProgressBar.VISIBLE
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            requestQueue.add(FilteredListRequest(
                "$serverUrl" + Urls.GET_LIST + "?start=${getStartPage()}" + "&pageSize=${Constants.PAGE_SIZE}" + "&sortName=${sortName}" + "&sortOrder=${sortOrder}" + "&cache=${getTargetCache()}",
                filterDto,
                { response ->
                    if (response.data?.data?.isEmpty() == true) {
                        handler.post {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            getEmptyListView().visibility = View.VISIBLE
                        }
                    } else {
                        response.data?.data?.let {
                            handler.post {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                getMainList().visibility = View.VISIBLE
                                val listAdapter = getMainList().adapter
                                if(listAdapter == null){
                                    getMainList().adapter = getListAdapter(it)
                                } else {
                                    (listAdapter as ArrayAdapter<T>).clear()
                                    getMainList().scheduleLayoutAnimation()
                                    listAdapter.addAll(it)
                                    listAdapter.notifyDataSetChanged()
                                }
                                getTotalListSizeTextView().visibility = View.VISIBLE
                                val totalListSize = response.data?.totalDataSize
                                totalListSize?.let {
                                    var upperBound = getStartPage() * Constants.PAGE_SIZE
                                    if (upperBound > totalListSize) upperBound = totalListSize
                                    val total = buildString {
                                        append(this@AbstractListActivity.getString(R.string.list_footer_from))
                                        append(" ")
                                        append(((getStartPage() - 1) * Constants.PAGE_SIZE + 1))
                                        append(" ")
                                        append(this@AbstractListActivity.getString(R.string.list_footer_to))
                                        append(" ")
                                        append(upperBound)
                                        append(" ")
                                        append(this@AbstractListActivity.getString(R.string.list_footer_total))
                                        append(" ")
                                        append(totalListSize)
                                    }
                                    getTotalListSizeTextView().text = total
                                    this.totalListSize = totalListSize
                                }
                            }
                        }
                    }
                },
                { error ->
                    handler.post {
                        makeErrorSnackBar(error)
                        getEmptyListView().visibility = View.VISIBLE
                        progressBar.visibility = ProgressBar.INVISIBLE
                    }
                },
                this@AbstractListActivity,
                getMainListType()
            ).also { it.tag = getRequestTag() })
        }
    }

    protected fun tryToHideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (e: Exception) {
            //No-op
        }
    }

    @SuppressLint("ApplySharedPref")
    protected fun logout() {
        MaterialAlertDialogBuilder(this@AbstractListActivity)
            .setTitle(this.getString(R.string.logout_confirmation))
            .setPositiveButton(R.string.action_logout) { _, _ ->
                requestQueue.add(
                    LogoutRequest(
                        NetworkUtils.getServerUrl(this),
                        {
                            PreferenceManager.getDefaultSharedPreferences(this)
                                .edit()
                                .remove(Constants.TOKEN_KEY)
                                .commit()
                            val intent = Intent()
                            intent.component = ComponentName(
                                this.packageName,
                                this.packageName + Constants.ACTIVE_LOGIN_COMPONENT
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        },
                        { error ->
                            makeErrorSnackBar(error)
                        }, this@AbstractListActivity
                    )
                )
            }
            .setNegativeButton(R.string.contact_deletion_cancel, null).show()
    }

    protected fun showSortSettingsDialogs() {
        val builderSortField = MaterialAlertDialogBuilder(this)
        builderSortField.setTitle(R.string.choose_sort_field).setItems(
            getFieldListDisplayNames()
        ) { dialogField, whichField ->
            sortName = this.resources.getStringArray(getFieldListObjNames())[whichField]
            dialogField.dismiss()
            val builderSortOrder = MaterialAlertDialogBuilder(this)
            builderSortOrder.setTitle(R.string.choose_sort_order).setItems(
                R.array.list_sort_order_display_names
            ) { dialogOrder, whichOrder ->
                sortOrder =
                    this.resources.getStringArray(R.array.list_sort_order_obj_names)[whichOrder]
                dialogOrder.dismiss()
                saveSortSettings()
                updateList(getFilter())
            }
            builderSortOrder.create().show()
        }
        builderSortField.create().show()
    }

    abstract fun getParentCoordinatorLayoutForSnackBar(): View

    abstract fun getEmptyListView(): TextView

    abstract fun getMainList(): ListView

    abstract fun getProgressBar(): ProgressBar

    abstract fun getStartPage(): Int

    abstract fun getTotalListSizeTextView(): TextView

    abstract fun getTargetCache(): String

    abstract fun getMainListType(): Type

    abstract fun getListAdapter(list: List<T>): ListAdapter

    abstract fun getFilter(): List<FilterDto>

    abstract fun getFieldListDisplayNames(): Int

    abstract fun getFieldListObjNames(): Int

    abstract fun saveSortSettings()
}