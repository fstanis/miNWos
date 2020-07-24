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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val DISPLAY_INFO_PERMISSION = Manifest.permission.READ_PHONE_STATE
val IS_DISPLAY_INFO_SUPPORTED = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

interface TelephonyStatusListener : DefaultLifecycleObserver {
    fun startListening()
    fun stopListening()
    fun setCallback(callback: (TelephonyStatus) -> Unit)
    fun clearCallback()

    override fun onStart(owner: LifecycleOwner) = startListening()
    override fun onStop(owner: LifecycleOwner) = stopListening()
}

class TelephonyStatusListenerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager
) : TelephonyStatusListener {
    private var telephonyStatus = TelephonyStatus()

    private var stateCallbackRegistered = false
    private var displayInfoCallbackRegistered = false
    private var callback: ((TelephonyStatus) -> Unit)? = null

    override fun startListening() {
        if (!stateCallbackRegistered) {
            telephonyManager.listen(
                stateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        or PhoneStateListener.LISTEN_SERVICE_STATE
            )
            stateCallbackRegistered = true
        }
        if (!displayInfoCallbackRegistered) {
            if (IS_DISPLAY_INFO_SUPPORTED && hasDisplayInfoPermission()) {
                telephonyManager.listen(
                    displayInfoListener,
                    PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
                )
                displayInfoCallbackRegistered = true
            }
        }
    }

    override fun stopListening() {
        if (stateCallbackRegistered) {
            telephonyManager.listen(stateListener, PhoneStateListener.LISTEN_NONE)
            stateCallbackRegistered = false
        }
        if (displayInfoCallbackRegistered) {
            telephonyManager.listen(displayInfoListener, PhoneStateListener.LISTEN_NONE)
            displayInfoCallbackRegistered = false
        }
    }

    override fun setCallback(callback: (TelephonyStatus) -> Unit) {
        this.callback = callback
    }

    override fun clearCallback() {
        callback = null
    }

    private fun hasDisplayInfoPermission() = ContextCompat.checkSelfPermission(
        context,
        DISPLAY_INFO_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("NewApi")
    private fun updateDisplayInfo(telephonyDisplayInfo: TelephonyDisplayInfo) {
        telephonyStatus = telephonyStatus.withDisplayInfo(telephonyDisplayInfo)
        update()
    }

    private fun updateNetworkState(state: Int, networkType: Int) {
        telephonyStatus = telephonyStatus.withNetworkState(state, networkType)
        update()
    }

    private fun updateServiceState(serviceState: ServiceState?) {
        telephonyStatus = telephonyStatus.withServiceState(serviceState)
        update()
    }

    private fun update() {
        callback?.invoke(telephonyStatus)
    }

    private val displayInfoListener = object : PhoneStateListener() {
        @SuppressLint("MissingPermission")
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) =
            updateDisplayInfo(telephonyDisplayInfo)
    }

    private val stateListener = object : PhoneStateListener() {
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) =
            updateNetworkState(state, networkType)

        override fun onServiceStateChanged(serviceState: ServiceState?) =
            updateServiceState(serviceState)
    }
}
