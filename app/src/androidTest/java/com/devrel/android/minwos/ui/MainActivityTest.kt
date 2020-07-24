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

package com.devrel.android.minwos.ui

import android.net.LinkProperties
import android.telephony.TelephonyManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.ConnectivityStatus
import com.devrel.android.minwos.data.ConnectivityStatus.NetworkData
import com.devrel.android.minwos.data.ConnectivityStatusListener
import com.devrel.android.minwos.data.DataListenersModule
import com.devrel.android.minwos.data.TelephonyStatus
import com.devrel.android.minwos.data.TelephonyStatusListener
import com.devrel.android.minwos.ui.main.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@UninstallModules(DataListenersModule::class)
@HiltAndroidTest
class MainActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @JvmField
    @BindValue
    val connectivityStatusListener: ConnectivityStatusListener = FakeConnectivityStatusListener()

    @JvmField
    @BindValue
    val telephonyStatusListener: TelephonyStatusListener = FakeTelephonyStatusListener()

    @Test
    fun displaysNetworks() {
        val network1 =
            NetworkData(0, linkProperties = LinkProperties().apply { interfaceName = "test0" })
        val network2 =
            NetworkData(1, linkProperties = LinkProperties().apply { interfaceName = "test1" })
        onView(withId(R.id.networksRecyclerView)).check(matches(hasChildCount(0)))
        connectivityCallback?.invoke(ConnectivityStatus(null, listOf(network1, network2)))
        onView(withId(R.id.networksRecyclerView)).check(
            matches(
                allOf(
                    hasChildCount(2),
                    hasDescendant(withText("test0")),
                    hasDescendant(withText("test1"))
                )
            )
        )
        connectivityCallback?.invoke(ConnectivityStatus(network2, listOf(network1, network2)))
        onView(withId(R.id.networksRecyclerView)).check(
            matches(
                allOf(
                    hasChildCount(2),
                    hasDescendant(withText("test1 (default)")),
                    hasDescendant(withText("test0"))
                )
            )
        )
    }

    @Test
    fun displaysPhoneStatus() {
        telephonyCallback?.invoke(TelephonyStatus())
        onView(withId(R.id.networkType)).check(matches(withText("UNKNOWN")))
        telephonyCallback?.invoke(TelephonyStatus(networkType = TelephonyManager.NETWORK_TYPE_CDMA))
        onView(withId(R.id.networkType)).check(matches(withText("CDMA")))
    }

    @Test
    fun refreshButton() {
        onView(withId(R.id.action_refresh)).perform(click())
        onView(withId(R.id.networksRecyclerView)).check(
            matches(
                allOf(
                    hasChildCount(1),
                    hasDescendant(withText("refresh"))
                )
            )
        )
    }

    var connectivityCallback: ((ConnectivityStatus) -> Unit)? = null
    var telephonyCallback: ((TelephonyStatus) -> Unit)? = null

    inner class FakeConnectivityStatusListener : ConnectivityStatusListener {
        override fun setCallback(callback: (ConnectivityStatus) -> Unit) {
            connectivityCallback = callback
        }

        override fun clearCallback() {
            connectivityCallback = null
        }

        override fun startListening() {}
        override fun stopListening() {}
        override fun refresh() {
            connectivityCallback?.invoke(
                ConnectivityStatus(
                    null,
                    listOf(
                        NetworkData(
                            0,
                            linkProperties = LinkProperties().apply { interfaceName = "refresh" })
                    )
                )
            )
        }
    }

    inner class FakeTelephonyStatusListener : TelephonyStatusListener {
        override fun setCallback(callback: (TelephonyStatus) -> Unit) {
            telephonyCallback = callback
        }

        override fun clearCallback() {
            telephonyCallback = null
        }

        override fun startListening() {}
        override fun stopListening() {}
    }
}
