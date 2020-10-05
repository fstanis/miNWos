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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.devrel.android.minwos.data.networks.ConnectivityStatus
import com.devrel.android.minwos.data.networks.ConnectivityStatusListener
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class NetworksViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var connectivityListener: ConnectivityStatusListener

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `refresh refreshes everything`() {
        val underTest = NetworksViewModel(connectivityListener)

        underTest.refresh()
        verify(connectivityListener, times(1)).refresh()
    }

    @Test
    fun `ConnectivityStatus in LiveData`() {
        var cbk: ((ConnectivityStatus) -> Unit)? = null
        val underTest = NetworksViewModel(object : ConnectivityStatusListener {
            override fun startListening() {}
            override fun stopListening() {}
            override fun refresh() {}
            override fun clearCallback() {}
            override fun setCallback(callback: (ConnectivityStatus) -> Unit) {
                cbk = callback
            }
        })
        val status = ConnectivityStatus(null, listOf())
        assertThat(cbk).isNotNull()
        assertThat(underTest.connectivityStatus.value).isNull()
        cbk!!(status)
        assertThat(underTest.connectivityStatus.value).isSameInstanceAs(status)
    }
}
