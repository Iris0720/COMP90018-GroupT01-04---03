package com.example.comp90018.domain.model

enum class ReportPeriod {
    WEEK,
    MONTH,
    YEAR,
    ALL
}

data class HistoryQuery(
    val period: ReportPeriod,
    val referenceTimeEpochMillis: Long,
    val timeZoneId: String
) {
    init {
        require(referenceTimeEpochMillis >= 0) { "Reference time must not be negative" }
        require(timeZoneId.isNotBlank()) { "Time zone id must not be blank" }
    }
}

sealed interface PeriodComparison {
    data object NoComparison : PeriodComparison

    data class PercentageChange(val percentage: Double) : PeriodComparison {
        init {
            require(percentage.isFinite()) { "Comparison percentage must be finite" }
        }
    }
}

data class ActivityReport(
    val query: HistoryQuery,
    val activityCount: Int,
    val totalDurationSeconds: Long,
    val totalDistanceMetres: Long,
    val healthCheckInCount: Int,
    val distanceComparison: PeriodComparison,
    val records: List<Record>
) {
    init {
        require(activityCount >= 0) { "Activity count must not be negative" }
        require(totalDurationSeconds >= 0) { "Total duration must not be negative" }
        require(totalDistanceMetres >= 0) { "Total distance must not be negative" }
        require(healthCheckInCount >= 0) { "Health check-in count must not be negative" }
    }
}
