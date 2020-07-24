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

package com.devrel.android.minwos.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.DISPLAY_INFO_PERMISSION
import com.devrel.android.minwos.data.IS_DISPLAY_INFO_SUPPORTED
import com.devrel.android.minwos.databinding.ActivityMainBinding
import com.devrel.android.minwos.service.ForegroundStatusService
import com.devrel.android.minwos.ui.help.HelpDialog
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var helpDialog: Lazy<HelpDialog>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = ConnectivityStatusAdapter()
        with(binding.networksRecyclerView) {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        viewModel.connectivityStatus.observe(this, Observer { networkStatus ->
            viewAdapter.connectivityStatus = networkStatus
        })
        viewModel.observeEvents(this)

        if (IS_DISPLAY_INFO_SUPPORTED) {
            binding.permissionsWarning.visibility = View.VISIBLE
            binding.overrideNetworkTypeRow.visibility = View.VISIBLE
            binding.globalInfo.setOnClickListener { permissionCheck.launch(DISPLAY_INFO_PERMISSION) }
        }
        viewModel.telephonyStatus.observe(this, Observer {
            binding.networkType.text = it.networkTypeString
            binding.networkState.text = it.networkStateString
            binding.nrState.text = it.nrState
            binding.overrideNetworkType.text = it.overrideNetworkTypeString
        })
        viewModel.permissionsGranted.observe(this, Observer { isGranted ->
            binding.permissionsWarning.takeIf { isGranted }?.visibility = View.GONE
        })
    }

    override fun onStart() {
        super.onStart()
        if (IS_DISPLAY_INFO_SUPPORTED) {
            viewModel.updatePermissions(
                ActivityCompat.checkSelfPermission(
                    this,
                    DISPLAY_INFO_PERMISSION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    private val permissionCheck =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.updatePermissions(isGranted)
        }

    private fun showHelp() {
        helpDialog.get().show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                showHelp()
                true
            }
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            R.id.action_notification -> {
                val intent = Intent(this, ForegroundStatusService::class.java)
                intent.action = ForegroundStatusService.ACTION_TOGGLE_FOREGROUND_SERVICE
                startForegroundService(intent)
                true
            }
            else -> false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return true
    }
}
