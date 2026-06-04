package com.healthconnect.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthconnect.analyzer.ui.MainViewModel
import com.healthconnect.analyzer.ui.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.loadData()
    }

    // Health Connect permission contract
    private val healthPermissionLauncher = registerForActivityResult(
        androidx.health.connect.client.permission.HealthPermission
            .createRequestPermissionResultContract()
    ) { _ ->
        viewModel.loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthAnalyzerScreen(
                        viewModel = viewModel,
                        onRequestPermissions = {
                            healthPermissionLauncher.launch(viewModel.requiredPermissions)
                        },
                        onInstallHealthConnect = {
                            HealthConnectClient.getOrCreate(this)
                        },
                        onShareFile = { file ->
                            val intent = com.healthconnect.analyzer.export.JsonExporter(this)
                                .shareFile(file)
                            startActivity(android.content.Intent.createChooser(intent, "Share Health Data"))
                        }
                    )
                }
            }
        }

        viewModel.checkAndLoad()
    }
}

@Composable
fun HealthAnalyzerScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onShareFile: (java.io.File) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Health Analyzer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (val s = state) {
            is UiState.Idle, is UiState.CheckingAvailability -> {
                CircularProgressIndicator()
                Text("Checking Health Connect...", modifier = Modifier.padding(top = 16.dp))
            }

            is UiState.NotAvailable -> {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Health Connect is not available on this device.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onInstallHealthConnect) {
                    Text("Install Health Connect")
                }
            }

            is UiState.RequestingPermissions -> {
                Text(
                    "This app needs permission to read your health data from Health Connect.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    "Permissions needed:\n• Steps\n• Heart Rate & HRV\n• Sleep\n• Workouts\n• Weight\n• Calories",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(onClick = onRequestPermissions) {
                    Text("Grant Permissions")
                }
            }

            is UiState.Loading -> {
                CircularProgressIndicator()
                Text(
                    "Reading your health data...\nThis may take a moment.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is UiState.Success -> {
                SummaryCard(summary = s.summary)
                Spacer(modifier = Modifier.height(16.dp))
                DataSourcesCard(sources = (s.summary["data_sources"] as? List<*>)?.map { it.toString() } ?: emptyList())
                Spacer(modifier = Modifier.height(16.dp))
                RecordCountsCard(counts = (s.summary["total_records"] as? Map<*, *>) ?: emptyMap<Any, Any>())
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onShareFile(s.exportedFile) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share JSON Export")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Exported to: ${s.exportedFile.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.checkAndLoad(30) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh (Last 30 days)")
                }
            }

            is UiState.Error -> {
                Text(
                    "Error: ${s.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.checkAndLoad() }) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun SummaryCard(summary: Map<String, Any>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Summary (Last ${summary["period_days"]} days)",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Avg Daily Steps", "${summary["avg_daily_steps"]}")
            StatRow("Avg Sleep", "${summary["avg_sleep_hours"]} hrs")
            StatRow("Avg Heart Rate", "${summary["avg_heart_rate_bpm"]} bpm")
            StatRow("Latest Weight", "${summary["latest_weight_kg"]} kg")
            StatRow("Total Workouts", "${summary["total_workouts"]}")
        }
    }
}

@Composable
fun DataSourcesCard(sources: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (sources.isEmpty()) {
                Text("No data sources found", style = MaterialTheme.typography.bodySmall)
            } else {
                sources.forEach { source ->
                    Text("• ${source.substringAfterLast(".")}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun RecordCountsCard(counts: Map<*, *>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Records Collected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            counts.forEach { (key, value) ->
                StatRow(key.toString().replace("_", " ").replaceFirstChar { it.uppercase() }, "$value")
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
