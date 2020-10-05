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

package com.devrel.android.minwos.data.phonestate

import android.telephony.ServiceState
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TelephonyStatusTest {
    private val baseTelephonyData =
        TelephonyStatus.TelephonyData(SubscriptionInfo(1, 0), SimInfo("", ""))

    @Test
    fun `uses data from TelephonyDisplayInfo`() {
        var underTest = baseTelephonyData
        assertThat(underTest.networkTypeString).isEqualTo("UNKNOWN")
        assertThat(underTest.overrideNetworkTypeString).isEqualTo("UNKNOWN")

        underTest = baseTelephonyData.copy(
            networkType = TelephonyManager.NETWORK_TYPE_CDMA,
            overrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
        )
        assertThat(underTest.networkTypeString).isEqualTo("CDMA")
        assertThat(underTest.overrideNetworkTypeString).isEqualTo("NR_NSA")
    }

    @Test
    fun `uses data from NetworkState`() {
        var underTest = baseTelephonyData
        assertThat(underTest.networkStateString).isEqualTo("DISCONNECTED")
        assertThat(underTest.networkTypeString).isEqualTo("UNKNOWN")
        underTest = baseTelephonyData.copy(
            networkState = TelephonyManager.DATA_SUSPENDED,
            networkType = TelephonyManager.NETWORK_TYPE_CDMA
        )
        assertThat(underTest.networkStateString).isEqualTo("SUSPENDED")
        assertThat(underTest.networkTypeString).isEqualTo("CDMA")
    }

    @Test
    fun `parses string from ServiceState`() {
        val base = baseTelephonyData
        val serviceState = mock(ServiceState::class.java)

        `when`(serviceState.toString()).thenReturn("nrState=NOT_RESTRICTED nrState=CONNECTED")
        assertThat(base.copy(serviceState = serviceState).nrState).isEqualTo("CONNECTED")

        `when`(serviceState.toString()).thenReturn("nrState=NOT_RESTRICTED")
        assertThat(base.copy(serviceState = serviceState).nrState).isEqualTo("NOT_RESTRICTED")

        `when`(serviceState.toString()).thenReturn("lorem ipsum")
        assertThat(base.copy(serviceState = serviceState).nrState).isEqualTo("UNKNOWN")
    }
}
