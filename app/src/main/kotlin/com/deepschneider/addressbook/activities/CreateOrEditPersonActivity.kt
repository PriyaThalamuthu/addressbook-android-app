package com.deepschneider.addressbook.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepschneider.addressbook.BuildConfig
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.adapters.ContactsListAdapter
import com.deepschneider.addressbook.adapters.DocumentsListAdapter
import com.deepschneider.addressbook.databinding.ActivityCreateOrEditPersonBinding
import com.deepschneider.addressbook.dto.ContactDto
import com.deepschneider.addressbook.dto.DocumentDto
import com.deepschneider.addressbook.dto.PageDataDto
import com.deepschneider.addressbook.dto.PersonDto
import com.deepschneider.addressbook.dto.TableDataDto
import com.deepschneider.addressbook.network.EntityGetRequest
import com.deepschneider.addressbook.network.SaveOrCreateEntityRequest
import com.deepschneider.addressbook.network.SimpleGetRequest
import com.deepschneider.addressbook.utils.Constants
import com.deepschneider.addressbook.utils.Urls
import com.deepschneider.addressbook.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CreateOrEditPersonActivity : AbstractEntityActivity() {
    private var downloadCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
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
                                    makeFileSnackBar(
                                        cursor.getString(columnTitle)
                                                + this@CreateOrEditPersonActivity.getString(R.string.downloading_finished_message),
                                        FileProvider.getUriForFile(
                                            this@CreateOrEditPersonActivity,
                                            BuildConfig.APPLICATION_ID + ".provider",
                                            File(path)
                                        ),
                                        mimeType
                                    )
                                }
                            } else {
                                makeSnackBar(cursor.getString(columnTitle)
                                        + this@CreateOrEditPersonActivity.getString(R.string.downloading_failed_message))
                            }
                        }
                    }
                } catch (exception: Exception) {
                    makeSnackBar(this@CreateOrEditPersonActivity.getString(R.string.downloading_failed_message))
                }
            }

            if (DownloadManager.ACTION_NOTIFICATION_CLICKED == intent.action) {
                val referenceId = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
                if (referenceId != null) {
                    if (referenceId.isNotEmpty()) {
                        val dmQuery = DownloadManager.Query()
                        dmQuery.setFilterById(*referenceId)
                        try {
                            downloadManager.query(dmQuery).use { cursor ->
                                if (cursor != null && cursor.count > 0) {
                                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                    val columnTitle = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                                    if (cursor.moveToFirst() && cursor.getInt(columnIndex) == DownloadManager.STATUS_RUNNING) {
                                        makeSnackBar(cursor.getString(columnTitle)
                                                + this@CreateOrEditPersonActivity.getString(R.string.downloading_in_progress_message))
                                    } else {
                                        makeSnackBar(cursor.getString(columnTitle)
                                                + this@CreateOrEditPersonActivity.getString(R.string.downloading_failed_message))
                                    }
                                }
                            }
                        } catch (exception: Exception) {
                            makeSnackBar(this@CreateOrEditPersonActivity.getString(R.string.downloading_failed_message))
                        }
                    }
                }
            }
        }
    }

    private lateinit var binding: ActivityCreateOrEditPersonBinding
    private var personDto: PersonDto? = null
    private lateinit var orgId: String
    private val fieldValidation = BooleanArray(4)
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var startFileChooserForResult: ActivityResultLauncher<Intent>
    private lateinit var currentContactList: MutableList<ContactDto>
    private lateinit var currentDocumentList: MutableList<DocumentDto>
    private var isFABOpen = false
    inner class TextFieldValidation(private val view: View) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            when (view.id) {
                R.id.first_name -> validateFirstNameEditText()
                R.id.last_name -> validateLastNameEditText()
                R.id.salary -> validateSalaryEditText()
                R.id.resume -> validateResumeEditText()
            }
            updateSaveButtonState()
        }
    }

    private fun showFABMenu() {
        isFABOpen = true
        binding.fabMain.animate().rotation(-150F)
        binding.fabContact.animate().translationY(-resources.getDimension(R.dimen.standard_75))
        personDto?.let {
            binding.fabFile.animate().translationY(-resources.getDimension(R.dimen.standard_125))
        }
    }

    fun closeFABMenu() {
        isFABOpen = false
        binding.fabMain.animate().rotation(0F)
        binding.fabContact.animate().translationY(0F)
        personDto?.let {
            binding.fabFile.animate().translationY(0F)
        }
    }

    private fun prepareFloatingActionButton() {
        binding.fabMain.setOnClickListener {
            if (!isFABOpen) {
                showFABMenu()
            } else {
                closeFABMenu()
            }
        }
        binding.fabContact.setOnClickListener {
            clearFocus()
            closeFABMenu()
            startForResult.launch(
                Intent(
                    applicationContext,
                    CreateOrEditContactActivity::class.java
                )
            )
        }
        binding.fabFile.setOnClickListener {
            var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
            chooseFile.type = "*/*"
            chooseFile = Intent.createChooser(chooseFile, "Choose a file")
            startFileChooserForResult.launch(chooseFile)
        }
    }

    private fun prepareExtras() {
        val extra = Utils.getSerializable(this, "person", PersonDto::class.java)
        if (extra != null) personDto = extra
    }

    private fun prepareLayout() {
        orgId = intent.getStringExtra("orgId").toString()
        binding.saveCreateButton.setOnClickListener {
            saveOrCreatePerson()
        }
        binding.contactsListView.setHasFixedSize(true)
        binding.contactsListView.layoutManager = LinearLayoutManager(this)
        binding.contactsListView.itemAnimator = DefaultItemAnimator()
        binding.documentsListView.setHasFixedSize(true)
        binding.documentsListView.layoutManager = object:LinearLayoutManager(this){
            override fun supportsPredictiveItemAnimations(): Boolean {
                return true
            }
        }
        binding.documentsListView.itemAnimator = DefaultItemAnimator()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_current_person, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!resources.configuration.isNightModeActive)
            setTheme(R.style.Theme_Addressbook_Light)
        super.onCreate(savedInstanceState)
        binding = ActivityCreateOrEditPersonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        prepareExtras()
        prepareLayout()
        prepareCurrencyEditText()
        updateUi(personDto)
        setupListeners()
        validateFirstNameEditText()
        validateLastNameEditText()
        validateSalaryEditText()
        validateResumeEditText()
        updateSaveButtonState()
        updateDocumentList()
        updateContactList()
        prepareFloatingActionButton()
        prepareLauncher()
    }

    private fun prepareCurrencyEditText() {
        binding.salaryCurrency.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this@CreateOrEditPersonActivity)
            builder.setTitle(R.string.choose_salary_currency).setItems(
                Constants.currencies.toTypedArray()
            ) { dialog, which ->
                binding.salaryCurrency.setText(Constants.currencies.toTypedArray()[which])
                dialog.dismiss()
            }
            builder.create().show()
        }
    }

    @Suppress("DEPRECATION")
    private fun prepareLauncher() {
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultContactDto = result.data?.extras?.get("contact") as ContactDto
                    val shouldDelete = result.data?.extras?.getBoolean("delete")
                    resultContactDto.id?.let {
                        if (shouldDelete == true) {
                            currentContactList.removeIf { x -> x.id == resultContactDto.id }
                        } else {
                            val originalContactDto = currentContactList.find { x -> x.id == resultContactDto.id }
                            originalContactDto?.data = resultContactDto.data
                            originalContactDto?.type = resultContactDto.type
                            originalContactDto?.description = resultContactDto.description
                        }
                    } ?: run {
                        currentContactList.add(resultContactDto)
                    }
                    if (currentContactList.isEmpty()) {
                        binding.emptyContactsList.visibility = View.VISIBLE
                        binding.contactsListView.visibility = View.GONE
                    } else {
                        binding.emptyContactsList.visibility = View.GONE
                        binding.contactsListView.visibility = View.VISIBLE
                    }
                    updateContactAdapter()
                }
            }
        startFileChooserForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val returnUri = result.data?.data
                    makeSnackBar(returnUri.toString())
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateContactAdapter() {
        val adapter = binding.contactsListView.adapter
        if (adapter != null) {
            (adapter as ContactsListAdapter).contacts = currentContactList
            adapter.notifyDataSetChanged()
        } else {
            binding.contactsListView.swapAdapter(
                ContactsListAdapter(
                    currentContactList,
                    this.resources.getStringArray(R.array.contact_types),
                    this@CreateOrEditPersonActivity,
                    startForResult
                ), false
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDocumentAdapter() {
        val adapter = binding.documentsListView.adapter
        if (adapter != null) {
            (adapter as DocumentsListAdapter).documents = currentDocumentList
            adapter.notifyDataSetChanged()
        } else {
            val newAdapter = DocumentsListAdapter(
                currentDocumentList,
                this@CreateOrEditPersonActivity
            )
            newAdapter.setHasStableIds(true)
            binding.documentsListView.swapAdapter(
                newAdapter, true
            )
        }
    }

    private fun updateSaveButtonState() {
        binding.saveCreateButton.isEnabled = fieldValidation.all { it }
    }

    fun clearFocus() {
        binding.lastName.clearFocus()
        binding.firstName.clearFocus()
        binding.resume.clearFocus()
        binding.salary.clearFocus()
    }

    private fun setupListeners() {
        binding.lastName.addTextChangedListener(TextFieldValidation(binding.lastName))
        binding.firstName.addTextChangedListener(TextFieldValidation(binding.firstName))
        binding.resume.addTextChangedListener(TextFieldValidation(binding.resume))
        binding.salary.addTextChangedListener(TextFieldValidation(binding.salary))
    }

    private fun validateFirstNameEditText() {
        val value = binding.firstName.text.toString().trim()
        if (value.isEmpty()) {
            binding.firstNameLayout.error = this.getString(R.string.validation_error_required_field)
            fieldValidation[3] = false
        } else if (value.length > 500) {
            binding.firstNameLayout.error = this.getString(R.string.validation_error_value_too_long)
            fieldValidation[3] = false
        } else {
            binding.firstNameLayout.error = null
            fieldValidation[3] = true
        }
    }

    private fun updateContactList() {
        personDto?.let {
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            executor.execute {
                requestQueue.add(
                    EntityGetRequest<TableDataDto<ContactDto>>(
                        "$serverUrl" + Urls.GET_CONTACTS + "?personId=${personDto?.id}",
                        { response ->
                            if (response.data?.data?.isEmpty() == true) {
                                handler.post {
                                    binding.emptyContactsList.visibility = View.VISIBLE
                                    currentContactList = arrayListOf()
                                }
                            } else {
                                response.data?.data?.let {
                                    handler.post {
                                        currentContactList = it.toMutableList()
                                        updateContactAdapter()
                                        binding.contactsListView.visibility = View.VISIBLE
                                        binding.emptyContactsList.visibility = View.GONE
                                    }
                                }
                            }
                        },
                        { error ->
                            handler.post {
                                makeErrorSnackBar(error)
                                binding.emptyContactsList.visibility = View.VISIBLE
                                binding.contactsListView.visibility = View.GONE
                            }
                        },
                        this@CreateOrEditPersonActivity,
                        object : TypeToken<PageDataDto<TableDataDto<ContactDto>>>() {}.type
                    ).also { it.tag = getRequestTag() })
            }
        } ?: run {
            currentContactList = arrayListOf()
            binding.emptyContactsList.visibility = View.VISIBLE
        }
    }

    private fun updateDocumentList() {
        personDto?.let {
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            executor.execute {
                requestQueue.add(
                    EntityGetRequest<TableDataDto<DocumentDto>>(
                        "$serverUrl" + Urls.GET_DOCUMENTS + "?personId=${personDto?.id}&origin=${serverUrl}",
                        { response ->
                            if (response.data?.data?.isEmpty() == true) {
                                handler.post {
                                    binding.documentsListView.visibility = View.GONE
                                    binding.emptyDocumentsList.visibility = View.VISIBLE
                                    currentDocumentList = arrayListOf()
                                }
                            } else {
                                response.data?.data?.let {
                                    handler.post {
                                        currentDocumentList = it.toMutableList()
                                        updateDocumentAdapter()
                                        binding.documentsListView.visibility = View.VISIBLE
                                        binding.emptyDocumentsList.visibility = View.GONE
                                    }
                                }
                            }
                        },
                        { error ->
                            handler.post {
                                makeErrorSnackBar(error)
                                binding.emptyDocumentsList.visibility = View.VISIBLE
                                binding.documentsListView.visibility = View.GONE
                            }
                        },
                        this@CreateOrEditPersonActivity,
                        object : TypeToken<PageDataDto<TableDataDto<DocumentDto>>>() {}.type
                    ).also { it.tag = getRequestTag() })
            }
        } ?: run {
            currentDocumentList = arrayListOf()
            binding.emptyDocumentsList.visibility = View.VISIBLE
        }
    }

    private fun validateLastNameEditText() {
        val value = binding.lastName.text.toString().trim()
        if (value.isEmpty()) {
            binding.lastNameLayout.error = this.getString(R.string.validation_error_required_field)
            fieldValidation[1] = false
        } else if (value.length > 500) {
            binding.lastNameLayout.error = this.getString(R.string.validation_error_value_too_long)
            fieldValidation[1] = false
        } else {
            binding.lastNameLayout.error = null
            fieldValidation[1] = true
        }
    }

    private fun validateSalaryEditText() {
        val value = binding.salary.text.toString().trim()
        if (value.isEmpty()) {
            binding.salaryLayout.error = this.getString(R.string.validation_error_required_field)
            fieldValidation[0] = false
        } else if (value.length > 100) {
            binding.salaryLayout.error = this.getString(R.string.validation_error_value_too_long)
            fieldValidation[0] = false
        } else {
            binding.salaryLayout.error = null
            fieldValidation[0] = true
        }
    }

    private fun saveOrCreatePerson() {
        var targetPersonDto: PersonDto? = null
        var create = false
        personDto?.let {
            targetPersonDto = it
            targetPersonDto?.firstName = binding.firstName.text.toString()
            targetPersonDto?.lastName = binding.lastName.text.toString()
            targetPersonDto?.resume = binding.resume.text.toString()
            targetPersonDto?.salary = binding.salary.text.toString() + " " + binding.salaryCurrency.text.toString()
        } ?: run {
            create = true
            targetPersonDto = PersonDto()
            targetPersonDto?.firstName = binding.firstName.text.toString()
            targetPersonDto?.lastName = binding.lastName.text.toString()
            targetPersonDto?.resume = binding.resume.text.toString()
            targetPersonDto?.salary = binding.salary.text.toString() + " " + binding.salaryCurrency.text.toString()
            targetPersonDto?.orgId = orgId
        }
        targetPersonDto?.let {
            val handler = Handler(Looper.getMainLooper())
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val url = "$serverUrl" + Urls.SAVE_OR_CREATE_PERSON
            executor.execute {
                requestQueue.add(
                    SaveOrCreateEntityRequest(
                        url,
                        it,
                        { response ->
                            response.data?.let { savedPersonDto ->
                                savedPersonDto.id?.let { personId ->
                                    sendLockRequest(
                                        true, Constants.PERSONS_CACHE_NAME, personId
                                    )
                                    requestQueue.add(SaveOrCreateEntityRequest(
                                        "$serverUrl" + Urls.SAVE_OR_CREATE_CONTACTS + "?personId=" + personId,
                                        currentContactList,
                                        { response ->
                                            response.data?.let {
                                                handler.post {
                                                    personDto = savedPersonDto
                                                    handler.post {
                                                        updateUi(personDto)
                                                        updateContactList()
                                                        clearFocus()
                                                    }
                                                    personDto?.id?.let {
                                                        if (create) {
                                                            makeSnackBar(
                                                                this@CreateOrEditPersonActivity.getString(
                                                                    R.string.person_created_message
                                                                )
                                                            )
                                                        } else {
                                                            makeSnackBar(
                                                                this@CreateOrEditPersonActivity.getString(
                                                                    R.string.changes_saved_message
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        { error ->
                                            handler.post {
                                                makeErrorSnackBar(error)
                                            }
                                        },
                                        this@CreateOrEditPersonActivity,
                                        object : TypeToken<PageDataDto<List<ContactDto>>>() {}.type
                                    ).also { it.tag = getRequestTag() })
                                }
                            }
                        },
                        { error ->
                            handler.post {
                                makeErrorSnackBar(error)
                            }
                        },
                        this@CreateOrEditPersonActivity,
                        object : TypeToken<PageDataDto<PersonDto>>() {}.type
                    ).also { it.tag = getRequestTag() })
            }
        }
    }

    private fun validateResumeEditText() {
        val value = binding.resume.text.toString().trim()
        if (value.isEmpty()) {
            binding.resumeLayout.error = this.getString(R.string.validation_error_required_field)
            fieldValidation[2] = false
        } else if (value.length > 2000) {
            binding.resumeLayout.error = this.getString(R.string.validation_error_value_too_long)
            fieldValidation[2] = false
        } else {
            binding.resumeLayout.error = null
            fieldValidation[2] = true
        }
    }

    override fun getParentCoordinatorLayoutForSnackBar(): View = binding.coordinatorLayout

    override fun getRequestTag(): String = "CREATE_OR_EDIT_PERSON_TAG"

    private fun updateUi(personDto: PersonDto?) {
        personDto?.let {
            binding.id.setText(it.id)
            binding.firstName.setText(it.firstName)
            binding.lastName.setText(it.lastName)
            it.salary?.let { salary ->
                binding.salary.setText(
                    salary.substring(
                        0,
                        salary.length - 4
                    )
                )
            }
            binding.resume.setText(it.resume)
            binding.saveCreateButton.text = this.getString(R.string.action_save_changes)
            title = " " + it.firstName + " " + it.lastName
            it.salary?.let { salary -> binding.salaryCurrency.setText(salary.substring(salary.length - 3)) }
        } ?: run {
            binding.saveCreateButton.text = this.getString(R.string.action_create)
            binding.salaryCurrency.setText(Constants.DEFAULT_CURRENCY)
        }
    }

    private fun getCurrentPersonAsHTML(): String {
        val main = StringBuilder()
            .append("<p>First name: ${binding.firstName.text}<br/>")
            .append("Last name: ${binding.lastName.text}<br/>")
            .append("Resume: ${binding.resume.text}<br/>")
            .append("Salary: ${binding.salary.text} ${binding.salaryCurrency.text}</p>")
        val contactTypes = this.resources.getStringArray(R.array.contact_types)
        if (currentContactList.isNotEmpty()) {
            main.append("<p>Contacts:</p><ol>")
            for (contact in currentContactList) {
                main.append("<li><div>")
                contact.type?.let { main.append("<p>${contactTypes[it.toInt() + 1]}<br/>") }
                main.append("${contact.data}<br/>")
                main.append("${contact.description}</p>")
                main.append("</div></li>")
            }
            main.append("</ol>")
        }
        return main.toString()
    }

    fun deleteDocument(documentDto: DocumentDto?) {
        MaterialAlertDialogBuilder(this@CreateOrEditPersonActivity)
            .setTitle(this.getString(R.string.delete_document_dialog_header))
            .setPositiveButton(R.string.contact_deletion_delete) { _, _ ->
                documentDto?.id?.let { documentId ->
                    val handler = Handler(Looper.getMainLooper())
                    val executor: ExecutorService = Executors.newSingleThreadExecutor()
                    val url = "$serverUrl" + Urls.DELETE_DOCUMENT + "?id=${documentId}"
                    executor.execute {
                        requestQueue.add(
                            SimpleGetRequest(
                                url,
                                {
                                    makeSnackBar(
                                        documentDto.name
                                                + " "
                                                + this.getString(R.string.deleted_document_notification)
                                    )
                                    updateDocumentList()
                                },
                                { error ->
                                    handler.post {
                                        makeErrorSnackBar(error)
                                    }
                                },
                                this@CreateOrEditPersonActivity
                            ).also { it.tag = getRequestTag() })
                    }

                }
            }
            .setNegativeButton(R.string.contact_deletion_cancel, null).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.action_share_person -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        Html.fromHtml(getCurrentPersonAsHTML(), Html.FROM_HTML_MODE_LEGACY)
                    )
                    type = "text/html"
                }
                startActivity(Intent.createChooser(sendIntent, null))
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        registerReceiver(downloadCompleteReceiver, intentFilter)
        personDto?.id?.let { sendLockRequest(true, Constants.PERSONS_CACHE_NAME, it) }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadCompleteReceiver)
        personDto?.id?.let { sendLockRequest(false, Constants.PERSONS_CACHE_NAME, it) }
    }
}