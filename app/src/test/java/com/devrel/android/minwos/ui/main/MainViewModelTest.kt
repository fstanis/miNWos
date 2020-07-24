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

package com.devrel.android.minwos.ui.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.devrel.android.minwos.data.ConnectivityStatus
import com.devrel.android.minwos.data.ConnectivityStatusListener
import com.devrel.android.minwos.data.TelephonyStatus
import com.devrel.android.minwos.data.TelephonyStatusListener
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var connectivityListener: ConnectivityStatusListener

    @Mock
    lateinit var telephonyListener: TelephonyStatusListener

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `observeEvents starts listening`() {
        val underTest = MainViewModel(connectivityListener, telephonyListener)

        val owner = mock(LifecycleOwner::class.java)
        val lifecycle = LifecycleRegistry(owner)
        `when`(owner.lifecycle).thenReturn(lifecycle)
        underTest.observeEvents(owner)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        verify(connectivityListener, times(1)).onStart(owner)
        verify(connectivityListener, times(1)).onStop(owner)
        verify(telephonyListener, times(1)).onStart(owner)
        verify(telephonyListener, times(1)).onStop(owner)
    }

    @Test
    fun `refresh refreshes everything`() {
        val underTest = MainViewModel(connectivityListener, telephonyListener)

        underTest.refresh()
        verify(connectivityListener, times(1)).refresh()
    }

    @Test
    fun `ConnectivityStatus in LiveData`() {
        var cbk: ((ConnectivityStatus) -> Unit)? = null
        val underTest = MainViewModel(object : ConnectivityStatusListener {
            override fun startListening() {}
            override fun stopListening() {}
            override fun refresh() {}
            override fun clearCallback() {}
            override fun setCallback(callback: (ConnectivityStatus) -> Unit) {
                cbk = callback
            }
        }, telephonyListener)
        val status = ConnectivityStatus(null, listOf())
        assertThat(cbk).isNotNull()
        assertThat(underTest.connectivityStatus.value).isNull()
        cbk!!(status)
        assertThat(underTest.connectivityStatus.value).isSameInstanceAs(status)
    }

    @Test
    fun `TelephonyStatus in LiveData`() {
        var cbk: ((TelephonyStatus) -> Unit)? = null
        val underTest = MainViewModel(connectivityListener, object : TelephonyStatusListener {
            override fun startListening() {}
            override fun stopListening() {}
            override fun clearCallback() {}
            override fun setCallback(callback: (TelephonyStatus) -> Unit) {
                cbk = callback
            }
        })
        val status = TelephonyStatus()
        assertThat(cbk).isNotNull()
        assertThat(underTest.telephonyStatus.value).isNull()
        cbk!!(status)
        assertThat(underTest.telephonyStatus.value).isSameInstanceAs(status)
    }
}
