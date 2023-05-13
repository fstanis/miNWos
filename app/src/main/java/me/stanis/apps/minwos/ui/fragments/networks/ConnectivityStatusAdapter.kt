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

package me.stanis.apps.minwos.ui.fragments.networks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.stanis.apps.minwos.R
import me.stanis.apps.minwos.data.networks.ConnectivityStatus
import me.stanis.apps.minwos.data.networks.ConnectivityStatus.NetworkData
import me.stanis.apps.minwos.databinding.ItemNetworkBinding
import me.stanis.apps.minwos.ui.util.alternateRowBackground
import me.stanis.apps.minwos.ui.util.formatBandwidth
import me.stanis.apps.minwos.ui.util.formatBoolean

class ConnectivityStatusAdapter : RecyclerView.Adapter<ConnectivityStatusAdapter.ViewHolder>() {
    var connectivityStatus = ConnectivityStatus(null, listOf())
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: ItemNetworkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        init {
            binding.capabilities.alternateRowBackground()
        }

        fun setNetworkData(networkData: NetworkData) = with(context) {
            val isDefault = networkData == connectivityStatus.defaultNetwork
            binding.title.text = getNetworkName(networkData.name, isDefault)
            binding.root.setBackgroundColor(getColor(isDefault))
            binding.cellular.text = formatBoolean(networkData.isCellular)
            binding.hasInternet.text = formatBoolean(networkData.hasInternet)
            binding.meteredness.text = formatBoolean(networkData.isNotMetered)
            binding.tempMeteredness.text = formatBoolean(networkData.isTemporarilyNotMetered)
            networkData.networkCapabilities.let {
                binding.downloadBandwidth.text = formatBandwidth(it?.linkDownstreamBandwidthKbps)
                binding.uploadBandwidth.text = formatBandwidth(it?.linkUpstreamBandwidthKbps)
            }
        }

        private fun getNetworkName(name: String, isDefault: Boolean) =
            if (isDefault) context.getString(R.string.default_network_template, name) else name

        private fun getColor(isDefault: Boolean) =
            if (isDefault) context.getColor(R.color.highlight) else Color.TRANSPARENT
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemNetworkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.setNetworkData(connectivityStatus.networks[position])

    override fun getItemCount(): Int = connectivityStatus.networks.size
    override fun getItemId(position: Int): Long = connectivityStatus.networks[position].id.toLong()
}
