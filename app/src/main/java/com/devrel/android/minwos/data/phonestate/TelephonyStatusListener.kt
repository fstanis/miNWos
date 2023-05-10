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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.devrel.android.minwos.data.util.RefreshFlow
import com.devrel.android.minwos.data.util.VibrationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.shareIn

val IS_DISPLAY_INFO_SUPPORTED = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

interface TelephonyStatusListener {
    val flow: SharedFlow<TelephonyStatus>
    fun refresh()
    fun recheckPermissions()
}

@ExperimentalCoroutinesApi
class TelephonyStatusListenerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val coroutineScope: CoroutineScope,
    private val vibrationHelper: VibrationHelper
) : TelephonyStatusListener {
    private val refreshFlow = RefreshFlow()
    private val hasPermissions = MutableStateFlow(hasPhoneStatePermission())

    override fun refresh() {
        refreshFlow.emit()
    }

    override fun recheckPermissions() {
        hasPermissions.value = hasPhoneStatePermission()
    }

    override val flow = callbackFlow {
        val manager = FlowManager(this@callbackFlow)
        manager.startListening()
        awaitClose { manager.stopListening() }
    }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(5000L), 1)

    fun hasPhoneStatePermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun getSubscriptionIds(): List<Int> {
        val ids = mutableSetOf<Int>()
        if (hasPhoneStatePermission()) {
            ids.addAll(
                subscriptionManager.activeSubscriptionInfoList?.map { it.subscriptionId }
                    ?: listOf()
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
            ).filter { it != INVALID_SUBSCRIPTION_ID }
        )
        return ids.toList()
    }

    private inner class FlowManager(
        private val sendChannel: SendChannel<TelephonyStatus>
    ) {
        private var refreshJob: Job? = null
        private val subscriptionMap = mutableMapOf<Int, TelephonyStatus.TelephonyData>()
        private val telephonyManagerMap = mutableMapOf<Int, TelephonyManager>()
        private val displayInfoListeners = mutableMapOf<Int, DisplayInfoListener>()
        private val stateListeners = mutableMapOf<Int, StateListener>()

        private val status
            get() = TelephonyStatus(subscriptionMap.values.sortedBy { it.subscription })

        private var isListening = false

        fun startListening() {
            subscriptionMap.keys.forEach { subscriptionId ->
                addListenersForSubscriptionId(subscriptionId)
            }
            isListening = true
            if (refreshJob == null) {
                refreshJob = Job().also {
                    coroutineScope.launch(it) { refreshFlow.collect { refresh() } }
                    coroutineScope.launch(it) { hasPermissions.collect { startListening() } }
                }
            }
        }

        fun stopListening() {
            subscriptionMap.keys.forEach { subscriptionId ->
                removeListenersForSubscriptionId(subscriptionId)
            }
            isListening = false
            refreshJob?.cancel()
            refreshJob = null
        }

        fun refresh() {
            vibrationHelper.tick()
            refreshSubscriptions()
            update()
            if (isListening) {
                startListening()
            }
        }

        init {
            refreshSubscriptions()
        }

        private fun refreshSubscriptions() {
            subscriptionMap.clear()
            getSubscriptionIds().forEach {
                val manager = telephonyManagerMap.getOrPut(
                    it,
                    { telephonyManager.createForSubscriptionId(it) }
                )
                val simInfo = SimInfo.getFromTelephonyManager(manager) ?: return@forEach
                subscriptionMap[it] =
                    TelephonyStatus.TelephonyData(SubscriptionInfo.getForId(it), simInfo)
            }
            telephonyManagerMap.keys.forEach { subscriptionId ->
                subscriptionId.takeIf { subscriptionMap.containsKey(it) }?.let {
                    removeListenersForSubscriptionId(it)
                }
            }
        }

        private fun addListenersForSubscriptionId(subscriptionId: Int) {
            val telephonyManager = telephonyManagerMap.get(subscriptionId) ?: return

            stateListeners.computeIfAbsent(subscriptionId) {
                // PhoneStateListener requires a Looper, so we have to construct it in main
                val stateListener = runBlocking(Dispatchers.Main) {
                    StateListener(subscriptionId)
                }
                telephonyManager.listen(
                    stateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        or PhoneStateListener.LISTEN_SERVICE_STATE
                        or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                )
                stateListener
            }
            displayInfoListeners
                .takeIf { IS_DISPLAY_INFO_SUPPORTED && hasPhoneStatePermission() }
                ?.computeIfAbsent(subscriptionId) {
                    // PhoneStateListener requires a Looper, so we have to construct it in main
                    val displayInfoListener = runBlocking(Dispatchers.Main) {
                        DisplayInfoListener(subscriptionId)
                    }
                    telephonyManager.listen(
                        displayInfoListener,
                        PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
                    )
                    displayInfoListener
                }
        }

        private fun removeListenersForSubscriptionId(subscriptionId: Int) {
            val telephonyManager = telephonyManagerMap[subscriptionId] ?: return

            stateListeners.computeIfPresent(subscriptionId) { _, listener ->
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                null
            }
            displayInfoListeners.computeIfPresent(subscriptionId) { _, listener ->
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                null
            }
        }

        @SuppressLint("NewApi")
        private fun updateDisplayInfo(
            subscriptionId: Int,
            telephonyDisplayInfo: TelephonyDisplayInfo
        ) {
            subscriptionMap.computeIfPresent(subscriptionId) { _, data ->
                data.copy(
                    networkType = telephonyDisplayInfo.networkType,
                    overrideNetworkType = telephonyDisplayInfo.overrideNetworkType
                )
            }
            update()
        }

        private fun updateNetworkState(subscriptionId: Int, state: Int, networkType: Int) {
            subscriptionMap.computeIfPresent(subscriptionId) { _, data ->
                data.copy(
                    networkState = state,
                    networkType = networkType
                )
            }
            update()
        }

        private fun updateServiceState(subscriptionId: Int, serviceState: ServiceState?) {
            subscriptionMap.computeIfPresent(subscriptionId) { _, data ->
                data.copy(serviceState = serviceState)
            }
            update()
        }

        private fun updateSignalStrength(subscriptionId: Int, signalStrength: SignalStrength?) {
            subscriptionMap.computeIfPresent(subscriptionId) { _, data ->
                data.copy(signalStrength = signalStrength)
            }
            update()
        }

        private fun update() {
            sendChannel.takeUnless { it.isClosedForSend }?.trySend(status)
        }

        private inner class DisplayInfoListener(private val subscriptionId: Int) :
            PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            @SuppressLint("MissingPermission")
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) =
                updateDisplayInfo(subscriptionId, telephonyDisplayInfo)
        }

        private inner class StateListener(private val subscriptionId: Int) : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onDataConnectionStateChanged(state: Int, networkType: Int) =
                updateNetworkState(subscriptionId, state, networkType)

            @Deprecated("Deprecated in Java")
            override fun onServiceStateChanged(serviceState: ServiceState?) =
                updateServiceState(subscriptionId, serviceState)

            @Deprecated("Deprecated in Java")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) =
                updateSignalStrength(subscriptionId, signalStrength)
        }
    }
}
