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

package com.devrel.android.minwos.data

import com.devrel.android.minwos.data.networks.ConnectivityStatusListener
import com.devrel.android.minwos.data.networks.ConnectivityStatusListenerImpl
import com.devrel.android.minwos.data.phonestate.TelephonyStatusListener
import com.devrel.android.minwos.data.phonestate.TelephonyStatusListenerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class DataListenersActivityModule {
    @ActivityScoped
    @Binds
    abstract fun bindConnectivityStatusListener(impl: ConnectivityStatusListenerImpl):
        ConnectivityStatusListener

    @ActivityScoped
    @Binds
    abstract fun bindTelephonyStatusListener(impl: TelephonyStatusListenerImpl):
        TelephonyStatusListener
}

@Module
@InstallIn(ServiceComponent::class)
abstract class DataListenersServiceModule {
    @Binds
    abstract fun bindConnectivityStatusListener(impl: ConnectivityStatusListenerImpl):
        ConnectivityStatusListener
}
