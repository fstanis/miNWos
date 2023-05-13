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

package me.stanis.apps.minwos.ui.util

import android.content.Context
import me.stanis.apps.minwos.R
import java.text.NumberFormat

fun Context.formatBoolean(boolean: Boolean?) = when (boolean) {
    null -> getString(R.string.state_unknown)
    true -> getString(R.string.state_yes)
    false -> getString(R.string.state_no)
}

fun Context.formatBandwidth(bandwidthKbps: Int?) = when (bandwidthKbps) {
    null -> getString(R.string.state_unknown)
    in 0..999 -> getString(R.string.bandwidth_kbps, bandwidthKbps)
    else -> getString(R.string.bandwidth_mbps, bandwidthKbps.toFloat() / 1000)
}

fun Context.formatFrequency(frequencyKHz: Int?) = with(NumberFormat.getInstance()) {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
    isGroupingUsed = false
    when (frequencyKHz) {
        null -> getString(R.string.state_unknown)
        in 0..999 -> getString(R.string.frequency_khz, frequencyKHz)
        in 1000..999999 -> getString(R.string.frequency_mhz, format(frequencyKHz.toFloat() / 1000))
        else -> getString(R.string.frequency_ghz, format(frequencyKHz.toFloat() / 1000000))
    }
}
