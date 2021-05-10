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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devrel.android.minwos.data.phonestate.TelephonyStatus
import com.devrel.android.minwos.data.phonestate.TelephonyStatusListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PhoneStateViewModel @Inject constructor(
    private val telephonyStatusListener: TelephonyStatusListener
) : ViewModel() {
    private val telephonyStatusMutable = MutableLiveData<TelephonyStatus>()
    val telephonyStatus: LiveData<TelephonyStatus> get() = telephonyStatusMutable

    private val permissionsGrantedMutable = MutableLiveData<Boolean>(false)
    val permissionsGranted: LiveData<Boolean> get() = permissionsGrantedMutable

    init {
        telephonyStatusListener.setCallback { telephonyStatusMutable.postValue(it) }
    }

    fun refresh() = telephonyStatusListener.refresh()

    fun updatePermissions(granted: Boolean) {
        if (granted) {
            telephonyStatusListener.startListening()
        }
        permissionsGrantedMutable.postValue(granted)
    }

    override fun onCleared() {
        telephonyStatusListener.clearCallback()
        super.onCleared()
    }
}
