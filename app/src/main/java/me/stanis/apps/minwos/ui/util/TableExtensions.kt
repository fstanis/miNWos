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

package me.stanis.apps.minwos.ui.util

import android.graphics.Color
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.children
import androidx.core.view.isVisible
import me.stanis.apps.minwos.R

fun TableLayout.alternateRowBackground(): TableLayout {
    var rowIndex = 0
    this.children.forEach { row ->
        if (!row.isVisible || row !is TableRow) {
            return@forEach
        }
        row.setBackgroundColor(
            if (rowIndex % 2 == 0) {
                context.getColor(R.color.table_row_odd)
            } else {
                Color.TRANSPARENT
            },
        )
        rowIndex++
    }
    return this
}
