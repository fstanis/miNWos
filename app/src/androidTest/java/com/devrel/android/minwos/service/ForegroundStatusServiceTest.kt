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

package com.devrel.android.minwos.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.devrel.android.minwos.R
import com.devrel.android.minwos.ui.main.MainActivity
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class ForegroundStatusServiceTest {
    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activityManager = context.getSystemService(ActivityManager::class.java)
    }

    @Test
    fun toggleMenuItemStartsAndStopsService() {
        val toggleNotification = context.getString(R.string.toggle_notification)

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(toggleNotification)).perform(click())
        assertThat(serviceExists(ForegroundStatusService::class.java)).isTrue()

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(toggleNotification)).perform(click())
        assertThat(serviceExists(ForegroundStatusService::class.java)).isFalse()
    }

    @Suppress("DEPRECATION")
    private fun <T : Service> serviceExists(service: Class<T>) =
        activityManager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == service.name }
}
