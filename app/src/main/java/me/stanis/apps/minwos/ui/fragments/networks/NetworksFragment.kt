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

package me.stanis.apps.minwos.ui.fragments.networks

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import me.stanis.apps.minwos.R
import me.stanis.apps.minwos.databinding.FragmentNetworksBinding
import me.stanis.apps.minwos.service.ForegroundStatusService
import me.stanis.apps.minwos.ui.help.HelpDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NetworksFragment : Fragment() {
    private val viewModel: NetworksViewModel by viewModels()
    private lateinit var binding: FragmentNetworksBinding

    private val helpDialog by lazy {
        HelpDialog(
            requireContext(),
            getString(R.string.title_networks),
            getString(R.string.help_networks),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.action_menu_networks, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                this@NetworksFragment.onMenuItemSelected(menuItem)
        }, viewLifecycleOwner)
        binding = FragmentNetworksBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = ConnectivityStatusAdapter()
        with(binding.networksRecyclerView) {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectivityStatus.collect { networkStatus ->
                    viewAdapter.connectivityStatus = networkStatus
                }
            }
        }
    }

    private fun showHelp() {
        helpDialog.show()
    }

    private fun tryToggleService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionCheck.launch(POST_NOTIFICATIONS)
            return
        }
        toggleService()
    }

    private fun toggleService() {
        val intent = Intent(requireContext(), ForegroundStatusService::class.java)
        intent.action = ForegroundStatusService.ACTION_TOGGLE_FOREGROUND_SERVICE
        startForegroundService(requireContext(), intent)
    }

    private fun onMenuItemSelected(menuItem: MenuItem) =
        when (menuItem.itemId) {
            R.id.action_help -> {
                showHelp()
                true
            }

            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }

            R.id.action_notification -> {
                tryToggleService()
                true
            }

            else -> false
        }

    private val permissionCheck =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                toggleService()
            }
        }
}
