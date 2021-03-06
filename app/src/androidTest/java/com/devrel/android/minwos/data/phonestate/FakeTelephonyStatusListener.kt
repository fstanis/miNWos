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

class FakeTelephonyStatusListener : TelephonyStatusListener {
    var telephonyCallback: ((TelephonyStatus) -> Unit)? = null

    override fun setCallback(callback: (TelephonyStatus) -> Unit) {
        telephonyCallback = callback
    }

    override fun clearCallback() {
        telephonyCallback = null
    }

    override fun startListening() {}
    override fun stopListening() {}
    override fun refresh() {
        telephonyCallback?.invoke(
            TelephonyStatus(
                listOf(
                    TelephonyStatus.TelephonyData(SubscriptionInfo(99, 0), SimInfo("", "refresh"))
                )
            )
        )
    }
}
