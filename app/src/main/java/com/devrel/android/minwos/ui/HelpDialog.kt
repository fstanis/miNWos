package com.devrel.android.minwos.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.devrel.android.minwos.R

class HelpDialog(context: Context) : AlertDialog(context) {
    private val linkMovementMethod = LinkMovementMethod.getInstance()
    private val title = R.string.help
    private val message =
        HtmlCompat.fromHtml(context.getString(R.string.help_text), HtmlCompat.FROM_HTML_MODE_LEGACY)

    init {
        setTitle(title)
        setMessage(message)
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.button_ok)) { dialog, _ -> dialog.dismiss() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // makes links clickable
        findViewById<TextView>(android.R.id.message)?.movementMethod = linkMovementMethod
    }
}
