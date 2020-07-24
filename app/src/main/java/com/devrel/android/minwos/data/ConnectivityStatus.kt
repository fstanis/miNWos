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

import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities

data class ConnectivityStatus(
    val defaultNetwork: NetworkData?,
    private val allNetworks: List<NetworkData>
) {
    // ensure default network is first, if set
    val networks = defaultNetwork?.let { listOf(it).union(allNetworks).toList() } ?: allNetworks

    data class NetworkData(
        val id: Int,
        val networkCapabilities: NetworkCapabilities? = null,
        val linkProperties: LinkProperties? = null,
        val isBlocked: Boolean = false
    ) : Comparable<NetworkData> {
        constructor(
            network: Network,
            networkCapabilities: NetworkCapabilities? = null,
            linkProperties: LinkProperties? = null,
            isBlocked: Boolean = false
        ) : this(network.hashCode(), networkCapabilities, linkProperties, isBlocked)

        val name = linkProperties?.interfaceName ?: "<unknown>"
        val isCellular = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasInternet = networkCapabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        val isNotMetered =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isTemporarilyNotMetered =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)

        override fun compareTo(other: NetworkData): Int = name.compareTo(other.name)
    }
}
