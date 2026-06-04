package com.healthconnect.analyzer.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.healthconnect.analyzer.model.HealthSnapshot
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class JsonExporter(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val fileNameFormatter = DateTimeFormatter
        .ofPattern("yyyyMMdd_HHmmss")
        .withZone(ZoneId.systemDefault())

    fun export(snapshot: HealthSnapshot): File {
        val fileName = "health_data_${fileNameFormatter.format(Instant.now())}.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(gson.toJson(snapshot))
        return file
    }

    fun shareFile(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Health Connect Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getSummaryStats(snapshot: HealthSnapshot): Map<String, Any> {
        val avgSteps = if (snapshot.steps.isNotEmpty())
            snapshot.steps.groupBy { it.date }
                .mapValues { (_, entries) -> entries.maxOf { it.count } }
                .values.average().toLong()
        else 0L

        val avgSleepMinutes = if (snapshot.sleep.isNotEmpty())
            snapshot.sleep.map { it.durationMinutes }.average().toLong()
        else 0L

        val avgRestingHr = if (snapshot.heartRate.isNotEmpty())
            snapshot.heartRate.map { it.beatsPerMinute }.average().toLong()
        else 0L

        val latestWeight = snapshot.weight.lastOrNull()?.weightKg

        val sources = (
            snapshot.steps.map { it.sourceApp } +
            snapshot.heartRate.map { it.sourceApp } +
            snapshot.sleep.map { it.sourceApp } +
            snapshot.workouts.map { it.sourceApp }
        ).distinct()

        return mapOf(
            "period_days" to snapshot.periodDays,
            "exported_at" to snapshot.exportedAt,
            "avg_daily_steps" to avgSteps,
            "avg_sleep_hours" to String.format("%.1f", avgSleepMinutes / 60.0),
            "avg_heart_rate_bpm" to avgRestingHr,
            "latest_weight_kg" to (latestWeight ?: "N/A"),
            "total_workouts" to snapshot.workouts.size,
            "data_sources" to sources,
            "total_records" to mapOf(
                "steps" to snapshot.steps.size,
                "heart_rate_samples" to snapshot.heartRate.size,
                "sleep_sessions" to snapshot.sleep.size,
                "workouts" to snapshot.workouts.size,
                "weight_entries" to snapshot.weight.size,
                "hrv_entries" to snapshot.hrv.size
            )
        )
    }
}
