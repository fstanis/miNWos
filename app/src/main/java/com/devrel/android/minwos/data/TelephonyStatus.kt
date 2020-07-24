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

import android.os.Build
import android.telephony.ServiceState
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

data class TelephonyStatus(
    private val networkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN,
    private val overrideNetworkType: Int? = null,
    private val networkState: Int = TelephonyManager.DATA_DISCONNECTED,
    val nrState: String = nrStateFromServiceState(null)
) {
    val networkTypeString = networkTypeToString(networkType)
    val overrideNetworkTypeString = overrideNetworkTypeToString(overrideNetworkType)
    val networkStateString = networkStateToString(networkState)

    @RequiresApi(Build.VERSION_CODES.R)
    fun withDisplayInfo(displayInfo: TelephonyDisplayInfo) =
        TelephonyStatus(
            displayInfo.networkType,
            displayInfo.overrideNetworkType,
            networkState,
            nrState
        )

    fun withNetworkState(networkState: Int, networkType: Int) =
        TelephonyStatus(networkType, overrideNetworkType, networkState, nrState)

    fun withServiceState(serviceState: ServiceState?) =
        TelephonyStatus(
            networkType,
            overrideNetworkType,
            networkState,
            nrStateFromServiceState(serviceState)
        )

    companion object {
        private fun networkTypeToString(networkType: Int): String =
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
                TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                TelephonyManager.NETWORK_TYPE_NR -> "NR"
                else -> String.format("OTHER (%d)", networkType)
            }

        private fun overrideNetworkTypeToString(overrideNetworkType: Int?): String =
            when (overrideNetworkType) {
                null -> "UNKNOWN"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> "NONE"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE_CA"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE_ADVANCED_PRO"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "NR_NSA"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> "NR_NSA_MMWAVE"
                else -> String.format("OTHER (%d)", overrideNetworkType)
            }

        private fun networkStateToString(networkState: Int): String =
            when (networkState) {
                TelephonyManager.DATA_DISCONNECTED -> "DISCONNECTED"
                TelephonyManager.DATA_CONNECTING -> "CONNECTING"
                TelephonyManager.DATA_CONNECTED -> "CONNECTED"
                TelephonyManager.DATA_SUSPENDED -> "SUSPENDED"
                else -> String.format("OTHER (%d)", networkState)
            }

        private fun nrStateFromServiceState(serviceState: ServiceState?): String =
            serviceState?.toString()?.let {
                when {
                    it.contains(Regex("""\bnrState\s*=\s*CONNECTED\b""")) -> "CONNECTED"
                    it.contains(Regex("""\bnrState\s*=\s*NOT_RESTRICTED\b""")) -> "NOT_RESTRICTED"
                    it.contains(Regex("""\bnrState\s*=\s*RESTRICTED\b""")) -> "RESTRICTED"
                    it.contains(Regex("""\bnrState\s*=\s*NONE\b""")) -> "NONE"
                    else -> null
                }
            } ?: "UNKNOWN"
    }
}
