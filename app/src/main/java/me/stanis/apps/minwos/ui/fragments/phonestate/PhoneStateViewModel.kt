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

package me.stanis.apps.minwos.ui.fragments.phonestate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.stanis.apps.minwos.data.phonestate.TelephonyStatus
import me.stanis.apps.minwos.data.phonestate.TelephonyStatusListener
import me.stanis.apps.minwos.ui.util.VibrationHelper
import javax.inject.Inject

@HiltViewModel
class PhoneStateViewModel @Inject constructor(
    private val telephonyStatusListener: TelephonyStatusListener,
    private val vibrationHelper: VibrationHelper,
) : ViewModel() {
    private val permissionsGrantedFlow = MutableStateFlow(false)

    val state =
        telephonyStatusListener.flow.combine(permissionsGrantedFlow) { status, permissions ->
            State(status, permissions)
        }.stateIn(
            viewModelScope,
            WhileSubscribed(5_000),
            State(),
        )

    fun refresh() {
        if (telephonyStatusListener.refresh()) {
            vibrationHelper.tick()
        }
    }

    fun updatePermissions(granted: Boolean) {
        permissionsGrantedFlow.value = granted
        telephonyStatusListener.refresh()
    }

    data class State(
        val telephonyStatus: TelephonyStatus = TelephonyStatus.EMPTY,
        val permissionsGranted: Boolean = false,
    )
}
