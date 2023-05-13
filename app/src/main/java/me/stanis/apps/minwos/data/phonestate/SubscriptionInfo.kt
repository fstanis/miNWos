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

import android.os.Build
import android.telephony.SubscriptionManager

data class SubscriptionInfo(
    val id: Int,
    val simSlot: Int,
    val isDefault: Boolean = false,
    val isDefaultSms: Boolean = false,
    val isDefaultData: Boolean = false,
    val isDefaultVoice: Boolean = false,
    val isActiveData: Boolean? = false,
) : Comparable<SubscriptionInfo> {
    companion object {
        fun forId(id: Int) =
            SubscriptionInfo(
                id = id,
                simSlot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    SubscriptionManager.getSlotIndex(id)
                } else {
                    0
                },
                isDefault = SubscriptionManager.getDefaultSubscriptionId() == id,
                isDefaultSms = SubscriptionManager.getDefaultSmsSubscriptionId() == id,
                isDefaultData = SubscriptionManager.getDefaultDataSubscriptionId() == id,
                isDefaultVoice = SubscriptionManager.getDefaultVoiceSubscriptionId() == id,
                isActiveData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    SubscriptionManager.getActiveDataSubscriptionId() == id
                } else {
                    null
                },
            )
    }

    override fun compareTo(other: SubscriptionInfo): Int =
        when {
            isDefault -> -1
            other.isDefault -> 1
            else -> id.compareTo(other.id)
        }
}
