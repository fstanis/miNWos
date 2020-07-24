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

package com.devrel.android.minwos.ui.help

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.devrel.android.minwos.BuildConfig
import com.devrel.android.minwos.R
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class HelpDialog @Inject constructor(@ActivityContext context: Context) : AlertDialog(context) {
    private val linkMovementMethod = LinkMovementMethod.getInstance()
    private val title = context.getString(R.string.help_title, BuildConfig.VERSION_NAME)
    private val message = HtmlCompat.fromHtml(
        context.getString(R.string.help_text), HtmlCompat.FROM_HTML_MODE_LEGACY
    )

    init {
        setTitle(title)
        setMessage(message)
        setButton(
            DialogInterface.BUTTON_POSITIVE, context.getString(R.string.button_ok)
        ) { dialog, _ -> dialog.dismiss() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // makes links clickable
        findViewById<TextView>(android.R.id.message)?.movementMethod = linkMovementMethod
    }
}
