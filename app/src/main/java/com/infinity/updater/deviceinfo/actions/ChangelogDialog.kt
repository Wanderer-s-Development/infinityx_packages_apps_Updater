/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.infinity.updater.deviceinfo.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.infinity.updater.R
import java.util.concurrent.TimeUnit

sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Success(val changelog: String) : ChangelogUiState
    data class Error(val message: String) : ChangelogUiState
}

@Composable
fun ChangelogDialog(
    url: String,
    onDismissRequest: () -> Unit,
) {
    var state by remember { mutableStateOf<ChangelogUiState>(ChangelogUiState.Loading) }

    LaunchedEffect(url) {
        state = ChangelogUiState.Loading
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .callTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty().trim()
                        if (body.isNotEmpty()) {
                            state = ChangelogUiState.Success(body)
                        } else {
                            state = ChangelogUiState.Error("Changelog file is empty on GitHub.")
                        }
                    } else {
                        state = ChangelogUiState.Error("HTTP ${response.code}: Unable to load changelog from GitHub.")
                    }
                }
            } catch (e: Exception) {
                state = ChangelogUiState.Error(e.message ?: "Failed to connect to GitHub.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.show_changelog),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when (val currentState = state) {
                is ChangelogUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.changelog_loading),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                is ChangelogUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is ChangelogUiState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        SelectionContainer {
                            Text(
                                text = currentState.changelog,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        },
    )
}
