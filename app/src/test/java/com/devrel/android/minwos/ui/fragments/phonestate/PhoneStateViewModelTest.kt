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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.devrel.android.minwos.data.phonestate.TelephonyStatus
import com.devrel.android.minwos.data.phonestate.TelephonyStatusListener
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class PhoneStateViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var telephonyListener: TelephonyStatusListener

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `refresh refreshes everything`() {
        val underTest = PhoneStateViewModel(telephonyListener)

        underTest.refresh()
        verify(telephonyListener, times(1)).refresh()
    }

    @Test
    fun `TelephonyStatus in LiveData`() {
        var cbk: ((TelephonyStatus) -> Unit)? = null
        val underTest = PhoneStateViewModel(object : TelephonyStatusListener {
            override fun startListening() {}
            override fun stopListening() {}
            override fun refresh() {}
            override fun clearCallback() {}
            override fun setCallback(callback: (TelephonyStatus) -> Unit) {
                cbk = callback
            }
        })
        val status = TelephonyStatus(listOf())
        assertThat(cbk).isNotNull()
        assertThat(underTest.telephonyStatus.value).isNull()
        cbk!!(status)
        assertThat(underTest.telephonyStatus.value).isSameInstanceAs(status)
    }
}
