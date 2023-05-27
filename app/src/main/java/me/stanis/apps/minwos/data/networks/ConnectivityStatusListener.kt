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

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.stanis.apps.minwos.data.networks.ConnectivityStatus.NetworkData
import me.stanis.apps.minwos.data.util.RefreshFlow
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
    private val providerFlow = callbackFlow {
        var defaultNetwork: Long? = null
        val networkMap = mutableMapOf<Long, NetworkData>()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                networkMap.updateCapabilities(network, networkCapabilities)
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: LinkProperties,
            ) {
                networkMap.updateLinkProperties(network, linkProperties)
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                networkMap.updateBlockedStatus(network, blocked)
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
            }

            override fun onLost(network: Network) {
                networkMap.remove(network.networkHandle)
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
            }
        }
        val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                defaultNetwork = network.networkHandle
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
            }

            override fun onLost(network: Network) {
                defaultNetwork = null
                trySend(createConnectivityStatus(defaultNetwork, networkMap))
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

    override fun refresh() = refreshFlow.tryEmit()

    override val flow =
        merge(providerFlow, refreshFlow.map { getConnectivityStatus() }).distinctUntilChanged()

    private fun getConnectivityStatus(): ConnectivityStatus {
        val networkMap = mutableMapOf<Long, NetworkData>()
        for (network in connectivityManager.allNetworks) {
            connectivityManager.requestBandwidthUpdate(network)
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
            // always skip restricted networks
            if (
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            ) {
                continue
            }
            networkMap[network.networkHandle] = NetworkData(
                network,
                networkCapabilities = capabilities,
                linkProperties = connectivityManager.getLinkProperties(network),
            )
        }
        val defaultNetwork = connectivityManager.activeNetwork?.networkHandle
        return createConnectivityStatus(defaultNetwork, networkMap)
    }

    private fun MutableMap<Long, NetworkData>.updateCapabilities(
        network: Network,
        networkCapabilities: NetworkCapabilities,
    ) {
        this[network.networkHandle] =
            this[network.networkHandle]?.copy(networkCapabilities = networkCapabilities)
                ?: NetworkData(
                    network,
                    networkCapabilities = networkCapabilities,
                )
    }

    private fun MutableMap<Long, NetworkData>.updateLinkProperties(
        network: Network,
        linkProperties: LinkProperties,
    ) {
        this[network.networkHandle] =
            this[network.networkHandle]?.copy(linkProperties = linkProperties) ?: NetworkData(
                network,
                linkProperties = linkProperties,
            )
    }

    private fun MutableMap<Long, NetworkData>.updateBlockedStatus(
        network: Network,
        blocked: Boolean,
    ) {
        this[network.networkHandle] =
            this[network.networkHandle]?.copy(isBlocked = blocked) ?: NetworkData(
                network,
                isBlocked = blocked,
            )
    }

    private fun createConnectivityStatus(
        defaultNetwork: Long?,
        networkMap: Map<Long, NetworkData>,
    ) = ConnectivityStatus(networkMap[defaultNetwork], networkMap.values.toList())
}
