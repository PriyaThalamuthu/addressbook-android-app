package com.deepschneider.addressbook

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.deepschneider.addressbook.activities.LoginActivity
import com.deepschneider.addressbook.activities.OrganizationsActivity
import com.deepschneider.addressbook.utils.Constants
import junit.framework.TestCase.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityTest {
    @get:Rule
    public var loginActivityTestRule: ActivityTestRule<LoginActivity> = ActivityTestRule(
        LoginActivity::class.java
    )
    private var loginActivity: LoginActivity? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        loginActivity = loginActivityTestRule.activity
        PreferenceManager.getDefaultSharedPreferences(loginActivity).edit()
            .putBoolean(Constants.SETTINGS_SHOULD_USE_HTTP, true).commit()
        PreferenceManager.getDefaultSharedPreferences(loginActivity).edit()
            .putString(Constants.SETTINGS_SERVER_URL, "192.168.1.210:10000").commit()
        PreferenceManager.getDefaultSharedPreferences(loginActivity).edit()
            .remove(Constants.TOKEN_KEY).commit()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(loginActivity).edit()
            .remove(Constants.TOKEN_KEY).commit()
        loginActivity = null
    }

    @Test
    fun test() {
        // Log in
        onView(withId(R.id.edit_text_login)).perform(clearText(), typeText("admin"))
        onView(withId(R.id.edit_text_password)).perform(clearText(), typeText("adminPass"))

        // Check if organizations activity is open
        val monitor = InstrumentationRegistry.getInstrumentation()
            .addMonitor(OrganizationsActivity::class.java.name, null, false)
        onView(withId(R.id.login_button)).perform(click())
        val currentActivity = InstrumentationRegistry.getInstrumentation().waitForMonitor(monitor)
        assertNotNull(currentActivity)

        // Log out
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.action_logout)).perform(click())
    }
}
