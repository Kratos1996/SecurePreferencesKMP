package com.ishant.securepreferences.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishant.securepreferences.SecurePreferences
import kotlinx.coroutines.launch

@Composable
fun App(
    preferences: SecurePreferences,
    platformName: String,
) {
    val scope = rememberCoroutineScope()
    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Ready") }
    var storedKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun refreshKeysAndValues() {
        scope.launch {
            try {
                val keys = preferences.getAllKeys().sorted()
                val values = mutableMapOf<String, String>()
                for (k in keys) {
                    val strVal = preferences.getString(k, null)
                    values[k] = strVal ?: "<null>"
                }
                storedKeys = keys
                currentValues = values
            } catch (e: Exception) {
                statusMessage = "Error reading keys: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshKeysAndValues()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = "SecurePreferences KMP Sample",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Running on: $platformName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Status: $statusMessage",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(
                        onClick = {
                            if (keyInput.isBlank()) {
                                statusMessage = "Key cannot be blank"
                                return@Button
                            }
                            scope.launch {
                                try {
                                    preferences.putString(keyInput, valueInput)
                                    statusMessage = "Saved key: '$keyInput'"
                                    refreshKeysAndValues()
                                } catch (e: Exception) {
                                    statusMessage = "Error saving: ${e.message}"
                                }
                            }
                        },
                    ) {
                        Text("Save Key")
                    }

                    OutlinedButton(
                        onClick = {
                            if (keyInput.isBlank()) {
                                statusMessage = "Key cannot be blank"
                                return@OutlinedButton
                            }
                            scope.launch {
                                try {
                                    preferences.remove(keyInput)
                                    statusMessage = "Removed key: '$keyInput'"
                                    refreshKeysAndValues()
                                } catch (e: Exception) {
                                    statusMessage = "Error removing: ${e.message}"
                                }
                            }
                        },
                    ) {
                        Text("Remove Key")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    preferences.clear()
                                    statusMessage = "Cleared all preferences"
                                    refreshKeysAndValues()
                                } catch (e: Exception) {
                                    statusMessage = "Error clearing: ${e.message}"
                                }
                            }
                        },
                    ) {
                        Text("Clear All")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Stored Encrypted Preferences (${storedKeys.size}):",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (storedKeys.isEmpty()) {
                    Text(
                        text = "No preferences stored yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(storedKeys) { key ->
                            val value = currentValues[key] ?: ""
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                preferences.remove(key)
                                                statusMessage = "Deleted '$key'"
                                                refreshKeysAndValues()
                                            }
                                        },
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
