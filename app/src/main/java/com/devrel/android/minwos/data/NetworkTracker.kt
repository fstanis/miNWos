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

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NetworkTracker(private val connectivityManager: ConnectivityManager) {
    private var defaultNetwork: Network? = null
    private val networkMap = mutableMapOf<Network, NetworkData>()

    val networks
        get() = NetworkStatus(
            networkMap[defaultNetwork],
            networkMap.values.toList()
        )

    private val networksMutableLiveData = MutableLiveData<NetworkStatus>()
    val networksLiveData: LiveData<NetworkStatus> = networksMutableLiveData

    fun registerCallbacks() {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED).build(),
            NetworkCallback()
        )
        connectivityManager.registerDefaultNetworkCallback(DefaultNetworkCallback())
    }

    fun refresh() {
        for (network in connectivityManager.allNetworks) {
            val capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) != true) {
                continue
            }
            connectivityManager.requestBandwidthUpdate(network)
            networkMap[network] = NetworkData(
                network,
                capabilities,
                connectivityManager.getLinkProperties(network)
            )
        }
        update()
    }

    private fun updateNetworkCapabilities(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        networkMap[network] = networkMap[network]?.withNetworkCapabilities(networkCapabilities)
            ?: NetworkData(network, networkCapabilities = networkCapabilities)
        update()
    }

    private fun updateLinkProperties(network: Network, linkProperties: LinkProperties) {
        networkMap[network] = networkMap[network]?.withLinkProperties(linkProperties)
            ?: NetworkData(network, linkProperties = linkProperties)
        update()
    }

    private fun updateBlockedStatus(network: Network, blocked: Boolean) {
        networkMap[network] = networkMap[network]?.withBlocked(blocked)
            ?: NetworkData(network, blocked = blocked)
        update()
    }

    private fun removeNetwork(network: Network) {
        networkMap.remove(network)
        update()
    }

    private fun setNetworkDefault(network: Network) {
        defaultNetwork = network
        update()
    }

    private fun unsetNetworkDefault() {
        defaultNetwork = null
        update()
    }

    private fun update() {
        networksMutableLiveData.postValue(networks)
    }

    private inner class NetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = updateNetworkCapabilities(network, networkCapabilities)

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) =
            updateLinkProperties(network, linkProperties)

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) =
            updateBlockedStatus(network, blocked)

        override fun onLost(network: Network) = removeNetwork(network)
    }

    private inner class DefaultNetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = setNetworkDefault(network)
        override fun onLost(network: Network) = unsetNetworkDefault()
    }
}
