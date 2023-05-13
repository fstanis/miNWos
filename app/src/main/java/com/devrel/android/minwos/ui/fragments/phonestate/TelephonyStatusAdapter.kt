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

package com.devrel.android.minwos.ui.fragments.phonestate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.phonestate.IS_DISPLAY_INFO_SUPPORTED
import com.devrel.android.minwos.data.phonestate.SubscriptionInfo
import com.devrel.android.minwos.data.phonestate.TelephonyStatus
import com.devrel.android.minwos.databinding.ItemTelephonyBinding
import com.devrel.android.minwos.ui.util.alternateRowBackground
import com.devrel.android.minwos.ui.util.formatBoolean
import com.devrel.android.minwos.ui.util.formatFrequency

class TelephonyStatusAdapter :
    ListAdapter<TelephonyStatus.TelephonyData, TelephonyStatusAdapter.ViewHolder>
        (TelephonyStatus.TelephonyData.diffUtil) {
    inner class ViewHolder(private val binding: ItemTelephonyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        init {
            binding.overrideNetworkTypeRow.takeIf { IS_DISPLAY_INFO_SUPPORTED }?.visibility =
                View.VISIBLE
            binding.networkInfo.alternateRowBackground()
        }

        fun setTelephonyData(telephonyData: TelephonyStatus.TelephonyData) = with(context) {
            with(telephonyData.subscription) {
                binding.root.setBackgroundColor(getColor(this))
                binding.title.text = getTitle(this)
                binding.isDefaultDataSub.text = formatBoolean(isDefaultData)
                binding.isActiveDataSub.text = formatBoolean(isActiveData)
            }
            binding.simCarrier.text =
                telephonyData.sim.carrierName?.let { "$it (${telephonyData.sim.carrierId})" }
                    ?: getString(R.string.state_unknown)
            binding.networkType.text = telephonyData.networkTypeString
            binding.cellBandwidths.text =
                telephonyData.cellBandwidths?.joinToString("\n") { formatFrequency(it) }
                    ?: getString(R.string.state_unknown)
            binding.signalStrengths.text =
                telephonyData.signalStrengths?.joinToString("\n")
                    ?: getString(R.string.state_unknown)
            binding.networkState.text = telephonyData.networkStateString
            binding.nrState.text = telephonyData.nrState ?: getString(R.string.state_unknown)
            binding.overrideNetworkType.text =
                telephonyData.overrideNetworkTypeString ?: getString(R.string.state_unknown)
        }

        private fun getTitle(subscription: SubscriptionInfo) =
            context.getString(
                if (subscription.isDefault) {
                    R.string.default_subscription_template
                } else {
                    R.string.subscription_template
                },
                subscription.id,
            )

        private fun getColor(subscription: SubscriptionInfo) =
            if (subscription.isDefault) context.getColor(R.color.highlight) else Color.TRANSPARENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTelephonyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.setTelephonyData(getItem(position))
}
