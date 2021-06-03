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

import android.os.SystemClock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

/** A special type of hot flow that prevents "refresh spam" by dismissing refreshes emitted in a
 *  short time period after the last accepted refresh.
 */
class RefreshFlow(
    private val debounceTimeMillis: Long = 5_000L,
    private inline val getElapsedTime: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val backingFlow = MutableSharedFlow<Long>(0, 1, BufferOverflow.DROP_LATEST)

    fun emit() {
        backingFlow.tryEmit(getElapsedTime())
    }

    suspend fun collect(collector: FlowCollector<Unit>) {
        backingFlow.collect { timestamp ->
            // dismiss a buffered refresh that hasn't been accepted by any collector
            if (getElapsedTime() - timestamp <= 500) {
                collector.emit(Unit)
                // suspend refreshing for a few seconds to prevent spam
                delay(debounceTimeMillis)
            }
        }
    }

    suspend inline fun collect(crossinline action: suspend () -> Unit): Unit =
        collect(object : FlowCollector<Unit> {
            override suspend fun emit(value: Unit) = action()
        })
}
