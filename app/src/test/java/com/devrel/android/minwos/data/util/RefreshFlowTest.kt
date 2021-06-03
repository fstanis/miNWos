/*
 * Copyright 2021 Google LLC
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

package com.devrel.android.minwos.data.util

import com.google.common.truth.Truth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class RefreshFlowTest {
    @Test
    fun `an emit is collected`() =
        runBlocking {
            withTimeout(10_000) {
                val underTest = RefreshFlow(getElapsedTime = { 0 })
                var collected = 0
                val job = launch(Job()) { underTest.collect { collected++ } }
                delay(100)
                underTest.emit()
                delay(100)
                job.cancel()
                Truth.assertThat(collected).isEqualTo(1)
            }
        }

    @Test
    fun `consecutive emits are ignored`() =
        runBlocking {
            withTimeout(10_000) {
                val underTest = RefreshFlow(500, getElapsedTime = { 0 })
                var collected = 0
                val job = launch(Job()) { underTest.collect { collected++ } }
                delay(100)
                underTest.emit()
                underTest.emit()
                underTest.emit()
                delay(1000)
                underTest.emit()
                underTest.emit()
                delay(100)
                job.cancel()
                Truth.assertThat(collected).isEqualTo(2)
            }
        }
}
