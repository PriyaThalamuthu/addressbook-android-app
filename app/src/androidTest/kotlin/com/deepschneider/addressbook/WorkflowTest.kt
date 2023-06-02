package com.deepschneider.addressbook

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.deepschneider.addressbook.activities.CreateOrEditPersonActivity
import com.deepschneider.addressbook.activities.LoginActivity
import com.deepschneider.addressbook.utils.Constants
import junit.framework.TestCase
import org.hamcrest.core.IsAnything.anything
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WorkflowTest {
    @get:Rule
    public var loginActivityTestRule: ActivityTestRule<LoginActivity> =
        ActivityTestRule(LoginActivity::class.java)
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
            .putBoolean(Constants.SETTINGS_SHOW_LOCK_NOTIFICATIONS, false).commit()
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
        onView(withId(R.id.login_button)).perform(click())

        // Create new organization
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.name)).perform(clearText(), typeText("Apple"))
        onView(withId(R.id.address)).perform(clearText(), typeText("Test Address 111"))
        onView(withId(R.id.zip)).perform(clearText(), typeText("NN 111"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.type)).perform(click())
        onView(withText(R.string.choose_organization_type)).check(matches(isDisplayed()))
        onView(withText(R.string.organization_type_private)).perform(click())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_create_button)).perform(click())

        // Return back to organizations list
        Espresso.pressBack()

        // Sort by last updated in descending order
        onView(withId(R.id.action_sort_settings_organizations)).perform(click())
        onView(withText(R.string.choose_sort_field)).check(matches(isDisplayed()))
        onView(withText(R.string.search_org_last_updated)).perform(click())
        onView(withText(R.string.choose_sort_order)).check(matches(isDisplayed()))
        onView(withText(R.string.sort_order_desc)).perform(click())

        // Wait for list update from backend
        Thread.sleep(1000)

        // Click topmost organization
        onData(anything()).inAdapterView(withId(R.id.list_view)).atPosition(0).perform(click())

        // Create person
        val monitor = InstrumentationRegistry.getInstrumentation()
            .addMonitor(CreateOrEditPersonActivity::class.java.name, null, false)
        onView(withId(R.id.fab)).perform(click())
        val createPersonActivity = monitor.lastActivity
        TestCase.assertNotNull(createPersonActivity)
        onView(withId(R.id.first_name)).perform(clearText(), typeText("Steve"))
        onView(withId(R.id.last_name)).perform(clearText(), typeText("Jobs"))
        onView(withId(R.id.salary)).perform(clearText(), typeText("1000000"))
        onView(withId(R.id.resume)).perform(clearText(), typeText("1. First point\n2. Second point"))
        onView(withId(R.id.salary_currency)).perform(click())
        onView(withText(R.string.choose_salary_currency)).check(matches(isDisplayed()))
        onView(withText("AUD")).perform(click())

        // Create contact
        onView(withId(R.id.fab_main)).perform(click())
        onView(withId(R.id.fab_contact)).perform(click())

        onView(withId(R.id.type)).perform(click())
        onView(withText(R.string.choose_contact_type)).check(matches(isDisplayed()))
        onView(withText(R.string.contact_type_mobile_phone)).perform(click())
        onView(withId(R.id.data)).perform(clearText(), typeText("+1 999 999-99-99"))
        onView(withId(R.id.desc)).perform(clearText(), typeText("Personal iPhone"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.add_apply_button)).perform(click())
        onView(withText(R.string.action_create)).perform(scrollTo(), click())

        // Return back to persons list
        Espresso.pressBack()

        // Log out
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.action_logout)).perform(click())
    }
}