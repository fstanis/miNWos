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

package com.devrel.android.minwos.data

import android.telephony.ServiceState
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TelephonyStatusTest {
    @Test
    fun `uses data from TelephonyDisplayInfo`() {
        var underTest = TelephonyStatus()
        assertThat(underTest.networkTypeString).isEqualTo("UNKNOWN")
        assertThat(underTest.overrideNetworkTypeString).isEqualTo("UNKNOWN")

        val displayInfo = mock(TelephonyDisplayInfo::class.java)
        `when`(displayInfo.networkType).thenReturn(TelephonyManager.NETWORK_TYPE_CDMA)
        `when`(displayInfo.overrideNetworkType).thenReturn(TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA)
        underTest = underTest.withDisplayInfo(displayInfo)
        assertThat(underTest.networkTypeString).isEqualTo("CDMA")
        assertThat(underTest.overrideNetworkTypeString).isEqualTo("NR_NSA")
    }

    @Test
    fun `uses data from NetworkState`() {
        var underTest = TelephonyStatus()
        assertThat(underTest.networkStateString).isEqualTo("DISCONNECTED")
        assertThat(underTest.networkTypeString).isEqualTo("UNKNOWN")
        underTest = underTest.withNetworkState(
            TelephonyManager.DATA_SUSPENDED,
            TelephonyManager.NETWORK_TYPE_CDMA
        )
        assertThat(underTest.networkStateString).isEqualTo("SUSPENDED")
        assertThat(underTest.networkTypeString).isEqualTo("CDMA")
    }

    @Test
    fun `parses string from ServiceState`() {
        val base = TelephonyStatus()
        val serviceState = mock(ServiceState::class.java)

        `when`(serviceState.toString()).thenReturn("nrState=NOT_RESTRICTED nrState=CONNECTED")
        assertThat(base.withServiceState(serviceState).nrState).isEqualTo("CONNECTED")

        `when`(serviceState.toString()).thenReturn("nrState=NOT_RESTRICTED")
        assertThat(base.withServiceState(serviceState).nrState).isEqualTo("NOT_RESTRICTED")

        `when`(serviceState.toString()).thenReturn("lorem ipsum")
        assertThat(base.withServiceState(serviceState).nrState).isEqualTo("UNKNOWN")
    }
}
