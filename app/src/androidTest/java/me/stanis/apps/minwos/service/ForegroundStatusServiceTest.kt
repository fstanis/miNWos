/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.stanis.apps.minwos.service

import android.app.ActivityManager
import android.content.Context
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.stanis.apps.minwos.R
import me.stanis.apps.minwos.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class ForegroundStatusServiceTest {
    @get:Rule
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var uiDevice: UiDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activityManager = context.getSystemService(ActivityManager::class.java)
        activityRule.scenario.onActivity {
            val navHostFragment =
                it.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.nav_networks)
        }
    }

    @Test
    fun toggleMenuItemStartsAndStopsService() {
        val toggleNotification = context.getString(R.string.toggle_notification)

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(toggleNotification)).perform(click())
        uiDevice.openNotification()
        assertThat(notificationExists()).isTrue()
        uiDevice.pressBack()

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(toggleNotification)).perform(click())
        uiDevice.openNotification()
        assertThat(notificationExists()).isFalse()
        uiDevice.pressBack()
    }

    private fun notificationExists() =
        uiDevice.wait(
            Until.hasObject(
                By.res(context.resources.getResourceName(R.id.defaultNetworkNotification)),
            ),
            2000,
        )
}
