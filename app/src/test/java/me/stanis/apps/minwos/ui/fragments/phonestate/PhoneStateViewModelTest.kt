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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import me.stanis.apps.minwos.data.phonestate.SimInfo
import me.stanis.apps.minwos.data.phonestate.SubscriptionInfo
import me.stanis.apps.minwos.data.phonestate.TelephonyStatus
import me.stanis.apps.minwos.data.phonestate.TelephonyStatusListener
import me.stanis.apps.minwos.ui.util.VibrationHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class PhoneStateViewModelTest {
    @Mock
    lateinit var telephonyListener: TelephonyStatusListener

    private val vibrationHelper = mock(VibrationHelper::class.java)

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun `refresh refreshes everything`() {
        val underTest = PhoneStateViewModel(telephonyListener, vibrationHelper)

        underTest.refresh()
        verify(telephonyListener, times(1)).refresh()
    }

    @Test
    fun `TelephonyStatus in StateFlow`() = runBlocking {
        val flow = MutableSharedFlow<TelephonyStatus>(1)
        val underTest = PhoneStateViewModel(
            object : TelephonyStatusListener {
                override val flow get() = flow
                override fun refresh() = true
            },
            vibrationHelper,
        )

        val status = TelephonyStatus(
            listOf(
                TelephonyStatus.TelephonyData(SubscriptionInfo(1, 2), SimInfo("", "")),
            ),
        )
        flow.emit(status)

        // collect() to trigger the flow, because the StateFlow has WhileSubscribed()
        withTimeoutOrNull(100) { underTest.state.collect { } }
        assertThat(underTest.state.value.telephonyStatus).isSameInstanceAs(status)
        assertThat(underTest.state.value.permissionsGranted).isFalse()

        underTest.updatePermissions(true)
        withTimeoutOrNull(100) { underTest.state.collect { } }
        assertThat(underTest.state.value.telephonyStatus).isSameInstanceAs(status)
        assertThat(underTest.state.value.permissionsGranted).isTrue()
    }
}
