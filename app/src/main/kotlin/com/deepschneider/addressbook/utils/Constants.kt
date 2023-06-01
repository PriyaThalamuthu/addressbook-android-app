package com.deepschneider.addressbook.utils

import java.util.Currency
import java.util.Locale
import java.util.TreeSet

object Constants {
    const val MENU_DELETE_DOCUMENT = 101
    const val NO_VALUE = "no value"
    const val TOKEN_KEY = "token"
    const val BIOMETRICS = "biometrics"
    const val KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "addressbook_biometrics"
    const val SHARED_PREFERENCE_KEY_IV = "iv"

    const val ORGANIZATIONS_CACHE_NAME = "com.addressbook.model.Organization"
    const val PERSONS_CACHE_NAME = "com.addressbook.model.Person"

    var PAGE_SIZE = 10

    const val SETTINGS_SERVER_URL = "server_url"
    const val SETTINGS_ORGANIZATION_LIST_SORT_FIELD = "organization_list_sort_field"
    const val SETTINGS_ORGANIZATION_LIST_SORT_ORDER = "organization_list_sort_order"
    const val SETTINGS_SHOW_LOCK_NOTIFICATIONS = "show_lock_notifications"

    const val SETTINGS_PERSON_LIST_SORT_FIELD = "person_list_sort_field"
    const val SETTINGS_PERSON_LIST_SORT_ORDER = "person_list_sort_order"

    const val SETTINGS_SHOULD_USE_HTTP = "should_use_http"

    const val DEFAULT_CURRENCY = "USD"

    var currencies = TreeSet<String>()
    init {
        for (loc in Locale.getAvailableLocales()) {
            try {
                val res = Currency.getInstance(loc)
                if (res != null) currencies.add(res.currencyCode)
            } catch (exc: Exception) {
                //Np-op
            }
        }
    }

    var ACTIVE_LOGIN_COMPONENT: String? = null
}