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

package me.stanis.apps.minwos.data.phonestate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.stanis.apps.minwos.data.phonestate.SimInfo.Companion.simInfo
import me.stanis.apps.minwos.data.util.RefreshFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

val IS_DISPLAY_INFO_SUPPORTED = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

interface TelephonyStatusListener {
    val flow: Flow<TelephonyStatus>
    fun refresh(): Boolean
}

@ExperimentalCoroutinesApi
class TelephonyStatusListenerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
) : TelephonyStatusListener {
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    override fun refresh(): Boolean = refreshFlow.tryEmit()
    private val refreshFlow = RefreshFlow()

    private val subscriptionsChangeFlow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        callbackFlow {
            val listener = object :
                OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    super.onSubscriptionsChanged()
                    trySend(Unit)
                }
            }
            subscriptionManager.addOnSubscriptionsChangedListener(mainExecutor, listener)
            awaitClose {
                subscriptionManager.removeOnSubscriptionsChangedListener(listener)
            }
        }
    } else {
        emptyFlow()
    }

    override val flow = merge(refreshFlow, subscriptionsChangeFlow)
        .map { getSubscriptionIds() }
        .distinctUntilChanged()
        .map { subscriptionIds ->
            subscriptionIds
                .map {
                    TelephonyDataProvider(
                        telephonyManager,
                        mainExecutor,
                        it,
                    )
                }
                .filter { it.hasSim }
                .map { it.flow }
        }.flatMapLatest { flowList ->
            combine(flowList) { TelephonyStatus(it.toList()) }
        }

    private fun getSubscriptionIds(): Set<Int> {
        val ids = mutableSetOf<Int>()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ids.addAll(
                subscriptionManager.activeSubscriptionInfoList?.map { it.subscriptionId }
                    ?: listOf(),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var slot = 0
            while (true) {
                val slotIds = subscriptionManager.getSubscriptionIds(slot) ?: break
                ids.addAll(slotIds.asIterable())
                slot++
            }
        }
        ids.addAll(
            listOf(
                SubscriptionManager.getDefaultSubscriptionId(),
                SubscriptionManager.getDefaultDataSubscriptionId(),
                SubscriptionManager.getDefaultVoiceSubscriptionId(),
                SubscriptionManager.getDefaultSmsSubscriptionId(),
            ).filter { it != INVALID_SUBSCRIPTION_ID },
        )
        return ids
    }

    private class TelephonyDataProvider(
        telephonyManager: TelephonyManager,
        executor: Executor,
        subscriptionId: Int,
    ) {
        private val dispatcher = executor.asCoroutineDispatcher()
        private val subTelephonyManager = telephonyManager.createForSubscriptionId(subscriptionId)

        val hasSim = subTelephonyManager.simState == TelephonyManager.SIM_STATE_READY

        val flow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callbackFlow {
                var dataState = getInitialTelephonyData(subscriptionId)
                val callback = object :
                    TelephonyCallback(),
                    TelephonyCallback.DataConnectionStateListener,
                    TelephonyCallback.ServiceStateListener,
                    TelephonyCallback.SignalStrengthsListener,
                    TelephonyCallback.DisplayInfoListener {
                    override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                        dataState = dataState.updateDataConnectionState(state, networkType)
                        trySend(dataState)
                    }

                    override fun onServiceStateChanged(serviceState: ServiceState) {
                        dataState = dataState.updateServiceState(serviceState)
                        trySend(dataState)
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        dataState = dataState.updateSignalStrengths(signalStrength)
                        trySend(dataState)
                    }

                    override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                        dataState = dataState.updateDisplayInfo(telephonyDisplayInfo)
                        trySend(dataState)
                    }
                }
                subTelephonyManager.registerTelephonyCallback(executor, callback)
                awaitClose {
                    subTelephonyManager.unregisterTelephonyCallback(callback)
                }
            }
        } else {
            callbackFlow {
                var dataState = getInitialTelephonyData(subscriptionId)
                val listener = withContext(dispatcher) {
                    object : PhoneStateListener() {
                        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                            dataState = dataState.updateDataConnectionState(state, networkType)
                            trySend(dataState)
                        }

                        override fun onServiceStateChanged(serviceState: ServiceState) {
                            dataState = dataState.updateServiceState(serviceState)
                            trySend(dataState)
                        }

                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                            dataState = dataState.updateSignalStrengths(signalStrength)
                            trySend(dataState)
                        }

                        override fun onDisplayInfoChanged(
                            telephonyDisplayInfo: TelephonyDisplayInfo,
                        ) {
                            dataState = dataState.updateDisplayInfo(telephonyDisplayInfo)
                            trySend(dataState)
                        }
                    }
                }
                subTelephonyManager.listen(
                    listener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        or PhoneStateListener.LISTEN_SERVICE_STATE
                        or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        or PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED,
                )
                awaitClose {
                    subTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
            }
        }

        private fun TelephonyStatus.TelephonyData.updateDataConnectionState(
            state: Int,
            networkType: Int
        ) = copy(
            networkState = state,
            networkType = networkType,
        )

        private fun TelephonyStatus.TelephonyData.updateServiceState(serviceState: ServiceState) =
            copy(
                serviceState = serviceState,
            )


        private fun TelephonyStatus.TelephonyData.updateSignalStrengths(
            signalStrength: SignalStrength
        ) = copy(
            signalStrength = signalStrength,
        )

        private fun TelephonyStatus.TelephonyData.updateDisplayInfo(
            telephonyDisplayInfo: TelephonyDisplayInfo
        ) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                copy(
                    networkType = telephonyDisplayInfo.networkType,
                    overrideNetworkType = telephonyDisplayInfo.overrideNetworkType,
                )
            } else {
                this
            }

        private fun getInitialTelephonyData(subscriptionId: Int) = TelephonyStatus.TelephonyData(
            SubscriptionInfo.forId(subscriptionId),
            subTelephonyManager.simInfo,
        )
    }
}
