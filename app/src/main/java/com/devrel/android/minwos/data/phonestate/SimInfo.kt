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

import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID

data class SimInfo constructor(
    val country: String,
    val operator: String,
    val carrierId: Int = UNKNOWN_CARRIER_ID,
    val carrierName: String? = null,
) {
    companion object {
        val TelephonyManager.simInfo
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    SimInfo(
                        carrierId = simCarrierId,
                        carrierName = simCarrierIdName?.toString(),
                        country = simCountryIso,
                        operator = simOperatorName,
                    )
                } else {
                    SimInfo(
                        country = simCountryIso,
                        operator = simOperatorName,
                    )
                }
    }
}
