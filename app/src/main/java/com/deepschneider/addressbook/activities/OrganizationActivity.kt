package com.deepschneider.addressbook.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginTop
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.utils.NetworkUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class OrganizationActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var searchEditTextLastUpdated: EditText

    private lateinit var searchEditTextType: EditText

    private lateinit var requestQueue: RequestQueue

    private val lastUpdatedCalendar: Calendar = Calendar.getInstance()

    private val requestTag = "ORGANIZATIONS_TAG"

    private var serverUrl: String? = null

    private var start: Int = 1

    private var pageSize: Int = 15

    private var sortName: String = "id"

    private var sortOrder: String = "desc"

    private var targetCache: String = "com.addressbook.model.Organization"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organization)
        requestQueue = Volley.newRequestQueue(this)
        serverUrl = NetworkUtils.getServerUrl(this@OrganizationActivity)
        prepareActionBar()
        prepareFloatingActionButton()
        prepareSearchEditTextLastUpdated()
        prepareSearchEditTextType()
        updateUserInfo()
        updateBuildInfo()


    }

    private fun prepareSearchEditTextLastUpdated() {
        searchEditTextLastUpdated = findViewById(R.id.searchEditTextLastUpdated)
        searchEditTextLastUpdated.setOnClickListener {
            var isDataSet = false
            val dataPickerDialog = DatePickerDialog(
                this@OrganizationActivity,
                { _, year, month, day ->
                    with(lastUpdatedCalendar) {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    updateLabel()
                    isDataSet = true
                },
                lastUpdatedCalendar[Calendar.YEAR],
                lastUpdatedCalendar[Calendar.MONTH],
                lastUpdatedCalendar[Calendar.DAY_OF_MONTH]
            )
            dataPickerDialog.setOnDismissListener {
                if (!isDataSet) searchEditTextLastUpdated.text = null
            }
            dataPickerDialog.show()
        }
    }

    private fun prepareSearchEditTextType() {
        searchEditTextType = findViewById(R.id.searchEditTextType)
        searchEditTextType.setOnClickListener {
            val builder = AlertDialog.Builder(this@OrganizationActivity)
            builder.setTitle(R.string.choose_organization_type)
                .setItems(
                    R.array.org_types
                ) { dialog, which ->
                    if (which == 0)
                        searchEditTextType.text = null
                    else
                        searchEditTextType.setText(resources.getStringArray(R.array.org_types)[which])
                    dialog.dismiss()
                }
            builder.create().show()
        }
    }

    private fun prepareFloatingActionButton() {
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(applicationContext, CreateNewOrganizationActivity::class.java))
        }
    }

    private fun prepareActionBar() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerMain)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.drawer_opened,
            R.string.drawer_closed
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun updateBuildInfo() {
        requestQueue.add(object : JsonObjectRequest(
            Method.GET,
            "$serverUrl/rest/getBuildInfo",
            null,
            { response ->
                findViewById<TextView>(R.id.version_info).text =
                    "version: " + (response.get("version") as String?)?.uppercase()
                findViewById<TextView>(R.id.build_info).text =
                    "build: " + (response.get("time") as String?)?.uppercase()
                findViewById<TextView>(R.id.server_host).text =
                    "server host: " + (response.get("serverHost") as String?)?.uppercase()
            },
            { error ->
                makeErrorSnackBar(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return NetworkUtils.addAuthHeader(super.getHeaders(), this@OrganizationActivity)
            }
        }.also { it.tag = requestTag })
    }

    private fun updateUserInfo() {
        requestQueue.add(object : JsonObjectRequest(
            Method.GET,
            "$serverUrl/rest/getUserInfo",
            null,
            { response ->
                findViewById<TextView>(R.id.username).text =
                    (response.get("login") as String?)?.uppercase()
                val array = response.get("roles") as JSONArray
                val roles = arrayListOf<String>()
                for (i in 0 until array.length()) {
                    roles.add(array.get(i).toString())
                }
                findViewById<ListView>(R.id.rolesListView).adapter = ArrayAdapter(
                    this, android.R.layout.simple_list_item_1, roles
                )
            },
            { error ->
                makeErrorSnackBar(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return NetworkUtils.addAuthHeader(super.getHeaders(), this@OrganizationActivity)
            }
        }.also { it.tag = requestTag })
    }

    private fun updateLabel() {
        searchEditTextLastUpdated.setText(
            SimpleDateFormat(
                "MM/dd/yy",
                Locale.US
            ).format(lastUpdatedCalendar.time)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_organization, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return when (item.itemId) {
            R.id.action_logout_organizations -> {
                PreferenceManager.getDefaultSharedPreferences(this).edit().remove("token").commit()
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        findViewById<TextView>(R.id.server_info).text = "server: " +
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("server_url", "no value")
        super.onResume()
        updateOrganizationsList()
    }

    private fun updateOrganizationsList() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val url = "$serverUrl/rest/getList4UniversalListForm?" +
                "start=$start" +
                "&" +
                "pageSize=$pageSize" +
                "&" +
                "sortName=$sortName" +
                "&" +
                "sortOrder=$sortOrder" +
                "&" +
                "cache=$targetCache"
        executor.execute {
            requestQueue.add(object : JsonObjectRequest(
                Method.POST,
                url,
                null,
                { response ->
                    val organizationsListView = findViewById<ListView>(R.id.organizationsListView)
                    val organizations =
                        (response.get("data") as JSONObject).get("data") as JSONArray
                    val orgNames = arrayListOf<String>()
                    for (i in 0 until organizations.length()) {
                        orgNames.add((organizations.get(i) as JSONObject).get("name").toString())
                    }
                    handler.post {
                        organizationsListView.adapter = ArrayAdapter(
                            this, android.R.layout.simple_list_item_1, orgNames
                        )
                    }
                },
                { error ->
                    handler.post {
                        makeErrorSnackBar(error)
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = NetworkUtils.addAuthHeader(super.getHeaders(), this@OrganizationActivity)
                    headers["Content-Type"] = "application/json; charset=utf-8"
                    return headers
                }
            }.also { it.tag = requestTag })
        }
    }

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(requestTag)
    }

    private fun makeErrorSnackBar(error: VolleyError) {
        val snackBar = Snackbar.make(
            findViewById<CoordinatorLayout>(R.id.organizationsCoordinatorLayout),
            when (error) {
                is AuthFailureError -> "FORBIDDEN"
                is TimeoutError -> "SERVER CONNECTION TIMEOUT"
                is ServerError -> error.networkResponse?.data?.toString(Charsets.UTF_8)
                    ?: error.message.toString()
                else -> error.message.toString()
            },
            Snackbar.LENGTH_LONG
        )
        val view: View = snackBar.view
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.setMargins(
            0,
            (this@OrganizationActivity.resources.displayMetrics.density * 100).toInt(),
            0,
            0
        )
        view.layoutParams = params
        snackBar.show()
    }
}