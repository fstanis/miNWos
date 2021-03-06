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

package com.devrel.android.minwos.ui.fragments.networks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devrel.android.minwos.data.networks.ConnectivityStatus
import com.devrel.android.minwos.data.networks.ConnectivityStatusListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworksViewModel @Inject constructor(
    private val connectivityStatusListener: ConnectivityStatusListener
) : ViewModel() {
    private val connectivityStatusMutable = MutableLiveData<ConnectivityStatus>()
    val connectivityStatus: LiveData<ConnectivityStatus> get() = connectivityStatusMutable

    init {
        connectivityStatusListener.setCallback { connectivityStatusMutable.postValue(it) }
    }

    fun refresh() = connectivityStatusListener.refresh()

    override fun onCleared() {
        connectivityStatusListener.clearCallback()
        super.onCleared()
    }
}
