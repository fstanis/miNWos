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
    val networksLiveData: LiveData<NetworkStatus> get() = networksMutableLiveData

    private var callbacksRegistered = false

    fun registerCallbacks() {
        if (callbacksRegistered) {
            return
        }
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                // these capability filters are added by default
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build(),
            networkCallback
        )
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        callbacksRegistered = true
    }

    fun unregisterCallbacks() {
        if (!callbacksRegistered) {
            return
        }
        connectivityManager.unregisterNetworkCallback(networkCallback)
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        callbacksRegistered = false
    }

    fun refresh() {
        for (network in connectivityManager.allNetworks) {
            val capabilities = connectivityManager.getNetworkCapabilities(network);
            // always skip restricted networks
            if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {
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
        networkMap[network] = networkMap[network]?.copy(networkCapabilities = networkCapabilities)
            ?: NetworkData(network, networkCapabilities = networkCapabilities)
        update()
    }

    private fun updateLinkProperties(network: Network, linkProperties: LinkProperties) {
        networkMap[network] = networkMap[network]?.copy(linkProperties = linkProperties)
            ?: NetworkData(network, linkProperties = linkProperties)
        update()
    }

    private fun updateBlockedStatus(network: Network, blocked: Boolean) {
        networkMap[network] = networkMap[network]?.copy(isBlocked = blocked)
            ?: NetworkData(network, isBlocked = blocked)
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

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
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

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = setNetworkDefault(network)
        override fun onLost(network: Network) = unsetNetworkDefault()
    }
}
