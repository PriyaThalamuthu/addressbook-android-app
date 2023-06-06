package com.deepschneider.addressbook.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.preference.PreferenceManager
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.adapters.PersonsListAdapter
import com.deepschneider.addressbook.databinding.ActivityPersonBinding
import com.deepschneider.addressbook.dto.*
import com.deepschneider.addressbook.listeners.OnSwipeTouchListener
import com.deepschneider.addressbook.network.EntityGetRequest
import com.deepschneider.addressbook.utils.Constants
import com.deepschneider.addressbook.utils.Urls
import com.deepschneider.addressbook.utils.Utils
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.concurrent.Executors

class PersonsActivity : AbstractListActivity<PersonDto>() {

    private lateinit var binding: ActivityPersonBinding
    private var currentFilter: List<FilterDto>? = null
    private var start: Int = 1
    private lateinit var organizationDto: OrganizationDto

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!resources.configuration.isNightModeActive)
            setTheme(R.style.Theme_Addressbook_Light)
        super.onCreate(savedInstanceState)
        binding = ActivityPersonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        organizationDto = Utils.getSerializable(this,
            "organization",
            OrganizationDto::class.java
        ) as OrganizationDto
        title = organizationDto.name
        prepareListView()
        prepareActionBar(binding.drawerLayout)
        prepareFloatingActionButton()
        preparePersonSearchButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun prepareListView() {
        binding.listView.setOnItemClickListener { parent, _, position, _ ->
            val intent = Intent(applicationContext, CreateOrEditPersonActivity::class.java)
            val personDto = parent.adapter.getItem(position) as PersonDto
            intent.putExtra("person", personDto)
            intent.putExtra("orgId", organizationDto.id)
            startActivity(intent)
        }
        binding.listView.setOnTouchListener(object :
            OnSwipeTouchListener(this@PersonsActivity) {

            override fun onSwipeTop() {
                this@PersonsActivity.totalListSize?.let {
                    if (start * Constants.PAGE_SIZE < it) {
                        start++
                        updateList(getFilter())
                    }
                }
            }

            override fun onSwipeBottom() {
                if (start > 1) {
                    start--
                    updateList(getFilter())
                }
            }
        })
    }

    private fun preparePersonSearchButton() {
        binding.searchButton.setOnClickListener {
            mainDrawer.closeDrawer(GravityCompat.START)
            val filters = arrayListOf<FilterDto>()
            Utils.getTextFilterDto(
                this.getString(R.string.search_person_obj_id),
                this@PersonsActivity.binding.searchId.text.toString()
            )?.let { it1 -> filters.add(it1) }
            Utils.getTextFilterDto(
                this.getString(R.string.search_person_obj_first_name),
                this@PersonsActivity.binding.searchFirstName.text.toString()
            )?.let { it1 -> filters.add(it1) }
            Utils.getTextFilterDto(
                this.getString(R.string.search_person_obj_last_name),
                this@PersonsActivity.binding.searchLastName.text.toString()
            )?.let { it1 -> filters.add(it1) }
            Utils.getTextFilterDto(
                this.getString(R.string.search_person_obj_resume),
                this@PersonsActivity.binding.searchResume.text.toString()
            )?.let { it1 -> filters.add(it1) }
            Utils.getTextFilterDto(
                this.getString(R.string.search_person_obj_salary),
                this@PersonsActivity.binding.searchSalary.text.toString()
            )?.let { it1 -> filters.add(it1) }
            filters.add(getOrgIdFilterDto())
            currentFilter = filters
            tryToHideKeyboard()
            updateList(filters)
        }
    }

    private fun updateOrganization() {
        val handler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        val url = "$serverUrl" + Urls.GET_ORGANIZATION + "?id=${organizationDto.id}"
        executor.execute {
            requestQueue.add(
                EntityGetRequest<OrganizationDto>(
                    url,
                    { response ->
                        response?.let {
                            organizationDto = it
                        }
                        handler.post {
                            title = organizationDto.name
                        }
                    },
                    { error ->
                        handler.post {
                            makeErrorSnackBar(error)
                        }
                    },
                    this@PersonsActivity,
                    object : TypeToken<OrganizationDto>() {}.type
                ).also { it.tag = getRequestTag() })
        }
    }

    private fun getOrgIdFilterDto(): FilterDto {
        val orgIdFilterDto = FilterDto()
        orgIdFilterDto.name = "orgId"
        orgIdFilterDto.value = organizationDto.id
        orgIdFilterDto.comparator = ""
        orgIdFilterDto.type = "TextFilter"
        return orgIdFilterDto
    }

    private fun prepareFloatingActionButton() {
        binding.fab.setOnClickListener {
            val intent = Intent(applicationContext, CreateOrEditPersonActivity::class.java)
            intent.putExtra("orgId", organizationDto.id)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_person, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return when (item.itemId) {
            R.id.action_logout_persons -> {
                logout()
                return true
            }
            R.id.action_sort_settings_persons -> {
                showSortSettingsDialogs()
                return true
            }
            R.id.action_edit_organization -> {
                val intent = Intent(applicationContext, CreateOrEditOrganizationActivity::class.java)
                intent.putExtra("organization", organizationDto)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSortSettings()
        updateOrganization()
        updateList(getFilter())
    }

    @SuppressLint("ApplySharedPref")
    override fun saveSortSettings() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(Constants.SETTINGS_PERSON_LIST_SORT_FIELD, sortName).commit()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(Constants.SETTINGS_PERSON_LIST_SORT_ORDER, sortOrder).commit()
    }

    private fun loadSortSettings() {
        sortName = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(
                Constants.SETTINGS_PERSON_LIST_SORT_FIELD,
                this.getString(R.string.search_person_obj_id)
            )
            .toString()
        sortOrder = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.SETTINGS_PERSON_LIST_SORT_ORDER, "desc").toString()
    }

    override fun getRequestTag(): String = "PERSONS_TAG"

    override fun getParentCoordinatorLayoutForSnackBar(): CoordinatorLayout = binding.coordinatorLayout

    override fun getTargetCache(): String = Constants.PERSONS_CACHE_NAME

    override fun getMainList(): ListView = binding.listView

    override fun getStartPage(): Int = start

    override fun getEmptyListView(): TextView = binding.emptyList

    override fun getListAdapter(list: List<PersonDto>): ListAdapter = PersonsListAdapter(list, this@PersonsActivity)

    override fun getProgressBar(): ProgressBar = binding.progressBar

    override fun getTotalListSizeTextView(): TextView = binding.listTotalSize

    override fun getMainListType(): Type = object : TypeToken<PageDataDto<TableDataDto<PersonDto>>>() {}.type

    override fun getFilter(): List<FilterDto> = currentFilter ?: listOf(getOrgIdFilterDto())

    override fun getFieldListObjNames(): Int = R.array.persons_list_field_obj_names

    override fun getFieldListDisplayNames(): Int = R.array.persons_list_field_display_names
}