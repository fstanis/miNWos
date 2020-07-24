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

package com.devrel.android.minwos.ui.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormatExtensionsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFormatBoolean() {
        assertThat(context.formatBoolean(true)).ignoringCase().contains("yes")
        assertThat(context.formatBoolean(false)).ignoringCase().contains("no")
        assertThat(context.formatBoolean(null)).ignoringCase().contains("unknown")
    }

    @Test
    fun testFormatBandwidth() {
        assertThat(context.formatBandwidth(12)).isEqualTo("12 kbps")
        assertThat(context.formatBandwidth(1200)).isEqualTo("1.20 Mbps")
        assertThat(context.formatBandwidth(null)).ignoringCase().contains("unknown")
    }
}
