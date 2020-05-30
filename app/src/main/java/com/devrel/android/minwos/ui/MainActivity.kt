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

package com.devrel.android.minwos.ui

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.devrel.android.minwos.R
import com.devrel.android.minwos.data.NetworkTracker
import com.devrel.android.minwos.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val connectivityManager by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val helpDialog by lazy { HelpDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.swipeRefreshLayout.isRefreshing = true

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = ViewAdapterNetwork()
        with(binding.networksRecyclerView) {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        val networkTracker = NetworkTracker(connectivityManager)
        networkTracker.networksLiveData.observe(this, Observer { networkStatus ->
            viewAdapter.networkStatus = networkStatus
            binding.swipeRefreshLayout.isRefreshing = false
        })
        networkTracker.registerCallbacks();
        networkTracker.refresh()
        binding.swipeRefreshLayout.setOnRefreshListener {
            networkTracker.refresh()
        }
    }

    private fun showHelp() {
        helpDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                showHelp()
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
