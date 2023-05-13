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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A special type of hot flow that prevents "refresh spam" by dismissing refreshes emitted in a
 * short time period after the last accepted refresh.
 */
class RefreshFlow(private val throttlePeriod: Duration = DEFAULT_THROTTLE_PERIOD) : Flow<Unit> {
    private val backingFlow = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    fun tryEmit() = backingFlow.tryEmit(Unit)

    override suspend fun collect(collector: FlowCollector<Unit>) =
        backingFlow.throttle(throttlePeriod).collect(collector)

    companion object {
        private val DEFAULT_THROTTLE_PERIOD = 5.seconds
    }
}

/**
 * Drops emissions collected within [periodMillis] after the last emission.
 */
fun <T> Flow<T>.throttle(period: Duration): Flow<T> = flow {
    var lastTime = 0L
    collect { value ->
        val elapsedRealtime = SystemClock.elapsedRealtime()
        if (elapsedRealtime - lastTime >= period.inWholeMilliseconds) {
            lastTime = elapsedRealtime
            emit(value)
        }
    }
}
