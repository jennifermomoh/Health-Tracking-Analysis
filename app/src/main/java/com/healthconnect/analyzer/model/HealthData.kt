package com.healthconnect.analyzer.model

data class DailySteps(
    val date: String,
    val count: Long,
    val sourceApp: String
)

data class HeartRateSample(
    val timestamp: String,
    val beatsPerMinute: Long,
    val sourceApp: String
)

data class SleepSession(
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Long,
    val stages: List<SleepStage>,
    val sourceApp: String
)

data class SleepStage(
    val stage: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Long
)

data class WorkoutSession(
    val date: String,
    val startTime: String,
    val endTime: String,
    val exerciseType: String,
    val durationMinutes: Long,
    val activeCalories: Double?,
    val sourceApp: String
)

data class WeightEntry(
    val date: String,
    val weightKg: Double,
    val sourceApp: String
)

data class CalorieEntry(
    val date: String,
    val activeCalories: Double,
    val totalCalories: Double?,
    val sourceApp: String
)

data class HrvEntry(
    val timestamp: String,
    val hrv: Double,
    val sourceApp: String
)

data class HealthSnapshot(
    val exportedAt: String,
    val periodDays: Int,
    val steps: List<DailySteps>,
    val heartRate: List<HeartRateSample>,
    val sleep: List<SleepSession>,
    val workouts: List<WorkoutSession>,
    val weight: List<WeightEntry>,
    val calories: List<CalorieEntry>,
    val hrv: List<HrvEntry>
)
