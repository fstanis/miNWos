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

package com.devrel.android.minwos.ui.fragments.phonestate

import android.Manifest.permission.READ_PHONE_STATE
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.phonestate.IS_DISPLAY_INFO_SUPPORTED
import com.devrel.android.minwos.databinding.FragmentPhonestateBinding
import com.devrel.android.minwos.ui.help.HelpDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhoneStateFragment : Fragment() {
    private val viewModel: PhoneStateViewModel by viewModels()
    private lateinit var binding: FragmentPhonestateBinding

    private val helpDialog by lazy {
        HelpDialog(
            requireContext(),
            getString(R.string.title_phone_state),
            getString(R.string.help_phone_state)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        binding = FragmentPhonestateBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (IS_DISPLAY_INFO_SUPPORTED) {
            binding.permissionsWarning.visibility = View.VISIBLE
            binding.permissionsWarning.setOnClickListener {
                permissionCheck.launch(READ_PHONE_STATE)
            }
        }
        val viewManager = LinearLayoutManager(context)
        val viewAdapter = TelephonyStatusAdapter()
        with(binding.telephonyRecyclerView) {
            layoutManager = viewManager
            adapter = viewAdapter
        }
        lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    viewAdapter.submitList(state.telephonyStatus.subscriptions)
                    if (state.permissionsGranted) {
                        binding.permissionsWarning.visibility = View.GONE
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        if (IS_DISPLAY_INFO_SUPPORTED) {
            viewModel.updatePermissions(
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    private val permissionCheck =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.updatePermissions(isGranted)
        }

    private fun showHelp() {
        helpDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_help -> {
                showHelp()
                true
            }
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            else -> false
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.action_menu_phonestate, menu)
    }
}
