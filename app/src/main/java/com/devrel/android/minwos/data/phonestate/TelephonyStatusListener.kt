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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

val IS_DISPLAY_INFO_SUPPORTED = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

interface TelephonyStatusListener : DefaultLifecycleObserver {
    fun startListening()
    fun stopListening()
    fun refresh()
    fun setCallback(callback: (TelephonyStatus) -> Unit)
    fun clearCallback()

    override fun onStart(owner: LifecycleOwner) = startListening()
    override fun onStop(owner: LifecycleOwner) = stopListening()
}

class TelephonyStatusListenerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager
) : TelephonyStatusListener {
    private val subscriptionMap = mutableMapOf<Int, TelephonyStatus.TelephonyData>()
    private val telephonyManagerMap = mutableMapOf<Int, TelephonyManager>()
    private val displayInfoListeners = mutableMapOf<Int, DisplayInfoListener>()
    private val stateListeners = mutableMapOf<Int, StateListener>()

    private val status
        get() = TelephonyStatus(subscriptionMap.values.sortedBy { it.subscription })

    private var isListening = false
    private var callback: ((TelephonyStatus) -> Unit)? = null

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

    override fun refresh() {
        refreshSubscriptions()
        update()
        if (isListening) {
            startListening()
        }
    }

    override fun startListening() {
        subscriptionMap.keys.forEach { subscriptionId ->
            addListenersForSubscriptionId(subscriptionId)
        }
        isListening = true
    }

    override fun stopListening() {
        subscriptionMap.keys.forEach { subscriptionId ->
            removeListenersForSubscriptionId(subscriptionId)
        }
        isListening = false
    }

    private fun addListenersForSubscriptionId(subscriptionId: Int) {
        val telephonyManager = telephonyManagerMap.get(subscriptionId) ?: return

        stateListeners.computeIfAbsent(subscriptionId) {
            val stateListener = StateListener(subscriptionId)
            telephonyManager.listen(
                stateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    or PhoneStateListener.LISTEN_SERVICE_STATE
                    or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
            stateListener
        }
        displayInfoListeners.takeIf { IS_DISPLAY_INFO_SUPPORTED && hasPhoneStatePermission() }
            ?.computeIfAbsent(subscriptionId) {
                val displayInfoListener = DisplayInfoListener(subscriptionId)
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

    override fun setCallback(callback: (TelephonyStatus) -> Unit) {
        this.callback = callback
        update()
    }

    override fun clearCallback() {
        callback = null
    }

    private fun hasPhoneStatePermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("NewApi")
    private fun updateDisplayInfo(subscriptionId: Int, telephonyDisplayInfo: TelephonyDisplayInfo) {
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
        callback?.invoke(status)
    }

    private inner class DisplayInfoListener(private val subscriptionId: Int) :
        PhoneStateListener() {
        @SuppressLint("MissingPermission")
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) =
            updateDisplayInfo(subscriptionId, telephonyDisplayInfo)
    }

    private inner class StateListener(private val subscriptionId: Int) : PhoneStateListener() {
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) =
            updateNetworkState(subscriptionId, state, networkType)

        override fun onServiceStateChanged(serviceState: ServiceState?) =
            updateServiceState(subscriptionId, serviceState)

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) =
            updateSignalStrength(subscriptionId, signalStrength)
    }
}
