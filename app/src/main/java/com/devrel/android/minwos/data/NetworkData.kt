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

package com.devrel.android.minwos.data;

import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities;

data class NetworkData(
    private val network: Network,
    val networkCapabilities: NetworkCapabilities? = null,
    val linkProperties: LinkProperties? = null,
    val blocked: Boolean = false
) : Comparable<NetworkData> {
    val id = network.hashCode()
    val name = linkProperties?.interfaceName ?: "<unknown>"

    fun withNetworkCapabilities(networkCapabilities: NetworkCapabilities) = NetworkData(network, networkCapabilities, linkProperties, blocked)
    fun withLinkProperties(linkProperties: LinkProperties) = NetworkData(network, networkCapabilities, linkProperties, blocked)
    fun withBlocked(blocked: Boolean) = NetworkData(network, networkCapabilities, linkProperties, blocked)

    override fun compareTo(other: NetworkData): Int = name.compareTo(other.name)
}
