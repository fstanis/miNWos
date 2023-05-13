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

package com.devrel.android.minwos.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.networks.ConnectivityStatus
import com.devrel.android.minwos.service.ForegroundStatusService
import com.devrel.android.minwos.ui.util.formatBandwidth
import com.devrel.android.minwos.ui.util.formatBoolean
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NetworkNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val defaultNetworkChannelId = "default_network"

    fun createNetworkNotification(network: ConnectivityStatus.NetworkData?): Notification {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_network).apply {
            setTextViewText(R.id.title, network?.name ?: "<none>")
            setTextViewText(R.id.cellular, context.formatBoolean(network?.isCellular))
            setTextViewText(R.id.hasInternet, context.formatBoolean(network?.hasInternet))
            setTextViewText(R.id.meteredness, context.formatBoolean(network?.isNotMetered))
            setTextViewText(
                R.id.tempMeteredness,
                context.formatBoolean(network?.isTemporarilyNotMetered),
            )
            network?.networkCapabilities.let {
                setTextViewText(
                    R.id.downloadBandwidth,
                    context.formatBandwidth(it?.linkDownstreamBandwidthKbps),
                )
                setTextViewText(
                    R.id.uploadBandwidth,
                    context.formatBandwidth(it?.linkUpstreamBandwidthKbps),
                )
            }
        }
        val toggleIntent = PendingIntent.getService(
            context,
            0,
            ForegroundStatusService.getToggleIntent(context),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, defaultNetworkChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setOngoing(true)
            .addAction(R.drawable.ic_close, context.getString(R.string.dismiss), toggleIntent)
            .build()
    }

    fun createNotificationChannel() =
        NotificationChannel(
            defaultNetworkChannelId,
            context.getString(R.string.default_network),
            NotificationManager.IMPORTANCE_MIN,
        )
}
