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
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED
import android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.view.LayoutInflater
import android.view.View
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
            val cap = networkData.networkCapabilities

            binding.root.setBackgroundColor(
                if (networkData == networkStatus.defaultNetwork) {
                    context.getColor(R.color.colorHighlight)
                } else {
                    Color.TRANSPARENT
                }
            )
            binding.title.text = networkData.name
            binding.cellular.isChecked = cap?.hasTransport(TRANSPORT_CELLULAR) ?: false
            binding.hasInternet.isChecked = cap?.let {
                it.hasCapability(NET_CAPABILITY_INTERNET) && it.hasCapability(
                    NET_CAPABILITY_VALIDATED
                )
            } ?: false
            binding.meteredness.isChecked = cap?.hasCapability(NET_CAPABILITY_NOT_METERED) ?: false
            binding.tempMeteredness.isChecked =
                cap?.hasCapability(NET_CAPABILITY_TEMPORARILY_NOT_METERED) ?: false
            binding.downloadSpeed.text =
                context.getString(R.string.download_speed, cap?.linkDownstreamBandwidthKbps)
            binding.uploadSpeed.text =
                context.getString(R.string.upload_speed, cap?.linkUpstreamBandwidthKbps)
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
