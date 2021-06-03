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

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.AccessNetworkConstants
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.NetworkRegistrationInfo
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.recyclerview.widget.DiffUtil

@SuppressLint("NewApi")
data class TelephonyStatus(
    val subscriptions: List<TelephonyData>
) {
    data class TelephonyData(
        val subscription: SubscriptionInfo,
        val sim: SimInfo,
        val networkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN,
        val overrideNetworkType: Int? = null,
        val networkState: Int = TelephonyManager.DATA_DISCONNECTED,
        val serviceState: ServiceState? = null,
        val signalStrength: SignalStrength? = null
    ) {
        val networkTypeString
            get() = networkRegistrationInfo?.let { networkTypeToString(it.accessNetworkTechnology) }
                ?: networkTypeToString(networkType)
        val overrideNetworkTypeString
            get() = overrideNetworkType?.let {
                overrideNetworkTypeToString(it)
            }
        val networkStateString get() = networkStateToString(networkState)
        val nrState get() = networkRegistrationInfo?.toString()?.let { nrStateFromString(it) }
        val cellBandwidths: List<Int>?
            get() =
                serviceState
                    .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }
                    ?.cellBandwidths
                    ?.takeIf { it.isNotEmpty() }
                    ?.toList()
        val signalStrengths: List<String>?
            get() = signalStrength?.let { signalStrengthToString(it) }
        val networkRegistrationInfo
            get() =
                serviceState
                    .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
                    ?.networkRegistrationInfoList
                    ?.find {
                        it.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                            it.domain and NetworkRegistrationInfo.DOMAIN_PS != 0
                    }

        companion object {
            val diffUtil = object : DiffUtil.ItemCallback<TelephonyData>() {
                override fun areItemsTheSame(oldItem: TelephonyData, newItem: TelephonyData) =
                    oldItem.subscription.id == newItem.subscription.id

                override fun areContentsTheSame(oldItem: TelephonyData, newItem: TelephonyData) =
                    oldItem == newItem
            }
        }
    }

    companion object {
        val EMPTY = TelephonyStatus(listOf())

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

        private fun overrideNetworkTypeToString(overrideNetworkType: Int): String =
            when (overrideNetworkType) {
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

        private fun nrStateFromString(str: String): String? =
            when {
                str.contains(Regex("""\bnrState\s*=\s*CONNECTED\b""")) -> "CONNECTED"
                str.contains(Regex("""\bnrState\s*=\s*NOT_RESTRICTED\b""")) -> "NOT_RESTRICTED"
                str.contains(Regex("""\bnrState\s*=\s*RESTRICTED\b""")) -> "RESTRICTED"
                str.contains(Regex("""\bnrState\s*=\s*NONE\b""")) -> "NONE"
                else -> null
            }

        private fun signalStrengthToString(signalStrength: SignalStrength) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrength.cellSignalStrengths.map {
                    val level = "${signalLevelToString(it.level)} (ASU: ${it.asuLevel})"
                    when (it) {
                        is CellSignalStrengthCdma -> "CDMA: $level"
                        is CellSignalStrengthGsm -> "GSM: $level"
                        is CellSignalStrengthLte -> "LTE: $level"
                        is CellSignalStrengthNr -> "5G NR: $level"
                        is CellSignalStrengthTdscdma -> "TD-SCDMA: $level"
                        is CellSignalStrengthWcdma -> "WCDMA: $level"
                        else -> "Unknown type: $level"
                    }
                }
            } else {
                listOf(signalLevelToString(signalStrength.level))
            }

        private fun signalLevelToString(level: Int) =
            when (level) {
                CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN -> "NONE_OR_UNKNOWN"
                CellSignalStrength.SIGNAL_STRENGTH_POOR -> "POOR"
                CellSignalStrength.SIGNAL_STRENGTH_MODERATE -> "MODERATE"
                CellSignalStrength.SIGNAL_STRENGTH_GOOD -> "GOOD"
                CellSignalStrength.SIGNAL_STRENGTH_GREAT -> "GREAT"
                else -> String.format("OTHER (%d)", level)
            }
    }
}
