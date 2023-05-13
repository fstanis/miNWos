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

package me.stanis.apps.minwos.data.networks

import android.net.LinkProperties
import android.net.Network
import me.stanis.apps.minwos.data.networks.ConnectivityStatus.NetworkData
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ConnectivityStatusTest {
    @Test
    fun `default network is always first`() {
        val default = NetworkData(0)
        val other = NetworkData(1)
        val underTest = ConnectivityStatus(default, listOf(other, default))
        assertThat(underTest.networks).containsExactly(default, other).inOrder()
    }

    @Test
    fun `NetworkData id comes from Network`() {
        val network = mock(Network::class.java)
        assertThat(NetworkData(network).id).isEqualTo(network.hashCode())
    }

    @Test
    fun `NetworkData name comes from LinkProperties`() {
        val linkProperties = mock(LinkProperties::class.java)
        `when`(linkProperties.interfaceName).thenReturn("wlan5")
        var underTest = NetworkData(0)
        assertThat(underTest.name).ignoringCase().contains("unknown")
        underTest = underTest.copy(linkProperties = linkProperties)
        assertThat(underTest.name).isEqualTo("wlan5")
    }
}
