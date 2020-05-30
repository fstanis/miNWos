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

package com.devrel.android.minwos.ui;

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.NetworkData
import com.devrel.android.minwos.data.NetworkStatus
import com.devrel.android.minwos.databinding.ItemNetworkBinding

class ViewAdapterNetwork : RecyclerView.Adapter<ViewAdapterNetwork.ViewHolder>() {
    var networkStatus = NetworkStatus(null, listOf())
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: ItemNetworkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        @SuppressLint("InlinedApi")
        fun setNetworkData(networkData: NetworkData) {
            var name = networkData.name
            var color = Color.TRANSPARENT
            if (networkData == networkStatus.defaultNetwork) {
                name += " (default)"
                color = context.getColor(R.color.colorHighlight)
            }
            binding.root.setBackgroundColor(color)
            binding.title.text = name

            val cap = networkData.networkCapabilities
            binding.cellular.text =
                formatBoolean(cap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            binding.hasInternet.text = formatBoolean(cap?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            })
            binding.meteredness.text =
                formatBoolean(cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
            binding.tempMeteredness.text =
                formatBoolean(cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED))
            binding.downloadBandwidth.text = formatBandwidth(cap?.linkDownstreamBandwidthKbps)
            binding.uploadBandwidth.text = formatBandwidth(cap?.linkUpstreamBandwidthKbps)
        }

        private fun formatBoolean(state: Boolean?): String =
            when (state) {
                null -> context.getString(R.string.state_unknown)
                true -> context.getString(R.string.state_yes)
                false -> context.getString(R.string.state_no)
            }

        private fun formatBandwidth(bandwidth: Int?): String =
            when (bandwidth) {
                null -> context.getString(R.string.state_unknown)
                in 0..10000 -> context.getString(R.string.bandwidth_kbps, bandwidth)
                else -> context.getString(R.string.bandwidth_mbps, bandwidth.toFloat() / 1000)
            }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(ItemNetworkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.setNetworkData(networkStatus.networks[position])

    override fun getItemCount(): Int = networkStatus.networks.size
    override fun getItemId(position: Int): Long = networkStatus.networks[position].id.toLong()
}
