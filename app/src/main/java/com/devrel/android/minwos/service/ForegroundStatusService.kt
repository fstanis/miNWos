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

package com.devrel.android.minwos.service

import android.app.NotificationManager
import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.devrel.android.minwos.data.ConnectivityStatus
import com.devrel.android.minwos.data.ConnectivityStatusListener
import com.devrel.android.minwos.ui.notification.NetworkNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundStatusService : LifecycleService() {
    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var networkNotificationFactory: NetworkNotificationFactory

    @Inject
    lateinit var connectivityStatusListener: ConnectivityStatusListener

    companion object {
        const val ACTION_TOGGLE_FOREGROUND_SERVICE = "foreground.toggle"
        const val NOTIFICATION_ID = 1
    }

    private var started = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_FOREGROUND_SERVICE) {
            if (!started) {
                startForegroundService()
            } else {
                stopForegroundService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannel(networkNotificationFactory.createNotificationChannel())
        lifecycle.addObserver(connectivityStatusListener)
        connectivityStatusListener.setCallback { updateNotification(it.defaultNetwork) }
    }

    private fun stopForegroundService() {
        started = false
        stopForeground(true)
        stopSelf()
    }

    private fun startForegroundService() {
        val notification = networkNotificationFactory.createNetworkNotification(null)
        startForeground(NOTIFICATION_ID, notification)
        started = true
    }

    private fun updateNotification(network: ConnectivityStatus.NetworkData?) {
        notificationManager.notify(
            NOTIFICATION_ID,
            networkNotificationFactory.createNetworkNotification(network)
        )
    }
}
