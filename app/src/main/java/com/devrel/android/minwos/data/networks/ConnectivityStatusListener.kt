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

package com.devrel.android.minwos.data.networks

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.devrel.android.minwos.data.networks.ConnectivityStatus.NetworkData
import com.devrel.android.minwos.data.util.RefreshFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface ConnectivityStatusListener {
    val flow: Flow<ConnectivityStatus>
    fun refresh(): Boolean
}

@ExperimentalCoroutinesApi
class ConnectivityStatusListenerImpl @Inject constructor(
    private val connectivityManager: ConnectivityManager,
) : ConnectivityStatusListener {
    private val refreshFlow = RefreshFlow()
    private val providerFlow = ConnectivityStatusProvider(connectivityManager).flow

    override fun refresh() = refreshFlow.tryEmit()

    override val flow =
        merge(providerFlow, refreshFlow.map { getConnectivityStatus() }).distinctUntilChanged()

    private fun getConnectivityStatus(): ConnectivityStatus {
        val networkMap = mutableMapOf<Network, NetworkData>()
        for (network in connectivityManager.allNetworks) {
            connectivityManager.requestBandwidthUpdate(network)
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
            // always skip restricted networks
            if (
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            ) {
                continue
            }
            networkMap[network] = NetworkData(
                network,
                networkCapabilities = capabilities,
                linkProperties = connectivityManager.getLinkProperties(network),
            )
        }
        val defaultNetwork = connectivityManager.activeNetwork
        return ConnectivityStatus(networkMap[defaultNetwork], networkMap.values.toList())
    }

    private class ConnectivityStatusProvider(
        connectivityManager: ConnectivityManager,
    ) {
        private var defaultNetwork = MutableStateFlow<Network?>(null)
        private val networkMapState = MutableStateFlow(mapOf<Network, NetworkData>())
        private val connectivityStatus
            get() = ConnectivityStatus(
                networkMapState.value[defaultNetwork.value],
                networkMapState.value.values.toList(),
            )

        val flow = callbackFlow {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(updateCapabilities(network, networkCapabilities))
                }

                override fun onLinkPropertiesChanged(
                    network: Network,
                    linkProperties: LinkProperties,
                ) {
                    trySend(updateLinkProperties(network, linkProperties))
                }

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    trySend(updateBlockedStatus(network, blocked))
                }

                override fun onLost(network: Network) {
                    trySend(removeNetwork(network))
                }
            }
            val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(setNetworkDefault(network))
                }

                override fun onLost(network: Network) {
                    trySend(unsetNetworkDefault())
                }
            }
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    // these capability filters are added by default
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(),
                networkCallback,
            )
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
            }
        }

        private fun updateCapabilities(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ): ConnectivityStatus {
            networkMapState.update { networkMap ->
                val networkMapMutable = networkMap.toMutableMap()
                networkMapMutable[network] =
                    networkMap[network]?.copy(networkCapabilities = networkCapabilities)
                        ?: NetworkData(network, networkCapabilities = networkCapabilities)
                networkMapMutable
            }
            return connectivityStatus
        }

        private fun updateLinkProperties(
            network: Network,
            linkProperties: LinkProperties,
        ): ConnectivityStatus {
            networkMapState.update { networkMap ->
                val networkMapMutable = networkMap.toMutableMap()
                networkMapMutable[network] =
                    networkMap[network]?.copy(linkProperties = linkProperties)
                        ?: NetworkData(network, linkProperties = linkProperties)
                networkMapMutable
            }
            return connectivityStatus
        }

        private fun updateBlockedStatus(network: Network, blocked: Boolean): ConnectivityStatus {
            networkMapState.update { networkMap ->
                val networkMapMutable = networkMap.toMutableMap()
                networkMapMutable[network] =
                    networkMap[network]?.copy(isBlocked = blocked)
                        ?: NetworkData(network, isBlocked = blocked)
                networkMapMutable
            }
            return connectivityStatus
        }

        private fun removeNetwork(network: Network): ConnectivityStatus {
            networkMapState.update { networkMap ->
                networkMap.toMutableMap().also { it.remove(network) }
            }
            return connectivityStatus
        }

        private fun setNetworkDefault(network: Network): ConnectivityStatus {
            defaultNetwork.value = network
            return connectivityStatus
        }

        private fun unsetNetworkDefault(): ConnectivityStatus {
            defaultNetwork.value = null
            return connectivityStatus
        }
    }
}
