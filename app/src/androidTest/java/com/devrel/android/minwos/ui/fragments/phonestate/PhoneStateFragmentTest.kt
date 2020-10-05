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

package com.devrel.android.minwos.ui.fragments.phonestate

import android.telephony.TelephonyManager
import androidx.navigation.fragment.NavHostFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.DataListenersActivityModule
import com.devrel.android.minwos.data.DataListenersServiceModule
import com.devrel.android.minwos.data.networks.ConnectivityStatusListener
import com.devrel.android.minwos.data.networks.FakeConnectivityStatusListener
import com.devrel.android.minwos.data.phonestate.FakeTelephonyStatusListener
import com.devrel.android.minwos.data.phonestate.SimInfo
import com.devrel.android.minwos.data.phonestate.SubscriptionInfo
import com.devrel.android.minwos.data.phonestate.TelephonyStatus
import com.devrel.android.minwos.data.phonestate.TelephonyStatus.TelephonyData
import com.devrel.android.minwos.data.phonestate.TelephonyStatusListener
import com.devrel.android.minwos.ui.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@UninstallModules(DataListenersActivityModule::class, DataListenersServiceModule::class)
@HiltAndroidTest
class PhoneStateFragmentTest {
    @get:Rule
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @JvmField
    @BindValue
    val connectivityStatusListener: ConnectivityStatusListener = FakeConnectivityStatusListener()

    @JvmField
    @BindValue
    val telephonyStatusListener: TelephonyStatusListener = FakeTelephonyStatusListener()

    private val baseTelephonyData = TelephonyData(SubscriptionInfo(1, 0), SimInfo("", ""))
    private val telephonyCallback get() =
        (telephonyStatusListener as FakeTelephonyStatusListener).telephonyCallback

    @Before
    fun setUp() {
        activityRule.scenario.onActivity {
            val navHostFragment =
                it.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.nav_phone_state)
        }
    }

    @Test
    fun displaysPhoneState() {
        val data1 = baseTelephonyData.copy(networkType = TelephonyManager.NETWORK_TYPE_EDGE)
        val data2 = baseTelephonyData.copy(networkType = TelephonyManager.NETWORK_TYPE_LTE)
        onView(withId(R.id.telephonyRecyclerView)).check(matches(hasChildCount(0)))
        telephonyCallback?.invoke(TelephonyStatus(listOf(data1, data2)))
        onView(withId(R.id.telephonyRecyclerView)).check(
            matches(
                allOf(
                    hasChildCount(2),
                    hasDescendant(withText("EDGE")),
                    hasDescendant(withText("LTE"))
                )
            )
        )
    }

    @Test
    fun refreshButton() {
        onView(withId(R.id.action_refresh)).perform(click())
        onView(withId(R.id.telephonyRecyclerView)).check(
            matches(
                allOf(
                    hasChildCount(1),
                    hasDescendant(withText("Subscription #99"))
                )
            )
        )
    }
}
