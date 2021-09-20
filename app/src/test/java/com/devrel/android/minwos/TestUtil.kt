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

package com.devrel.android.minwos

import android.os.Build
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object TestUtil {
    private val sdkField = Build.VERSION::class.java.getField("SDK_INT")
    private val oldSdkValue = Build.VERSION.SDK_INT

    init {
        sdkField.isAccessible = true
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(sdkField, sdkField.modifiers and Modifier.FINAL.inv())
    }

    fun resetVersionSdkInt() = setVersionSdkInt(oldSdkValue)

    fun setVersionSdkInt(newValue: Int) {
        sdkField.set(null, newValue)
    }
}
