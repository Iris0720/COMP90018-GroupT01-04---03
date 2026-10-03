package com.example.comp90018.data.repository

import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.ReportPeriod
import java.util.Calendar
import java.util.TimeZone

internal data class HistoryWindow(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val previousStartEpochMillis: Long,
    val previousEndEpochMillis: Long
)

internal object HistoryWindowCalculator {
    private val knownTimeZones = TimeZone.getAvailableIDs().toSet()

    fun calculate(query: HistoryQuery): HistoryWindow {
        require(query.timeZoneId in knownTimeZones) { "Unknown time zone: ${query.timeZoneId}" }

        if (query.period == ReportPeriod.ALL) {
            return HistoryWindow(0, Long.MAX_VALUE, 0, 0)
        }

        val currentStart = startOfPeriod(query)
        val currentEnd = (currentStart.clone() as Calendar).apply { moveOnePeriod(query.period, 1) }
        val previousStart = (currentStart.clone() as Calendar).apply { moveOnePeriod(query.period, -1) }

        return HistoryWindow(
            startEpochMillis = currentStart.timeInMillis,
            endEpochMillis = currentEnd.timeInMillis,
            previousStartEpochMillis = previousStart.timeInMillis,
            previousEndEpochMillis = currentStart.timeInMillis
        )
    }

    private fun startOfPeriod(query: HistoryQuery): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone(query.timeZoneId)).apply {
            timeInMillis = query.referenceTimeEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            when (query.period) {
                ReportPeriod.WEEK -> {
                    firstDayOfWeek = Calendar.MONDAY
                    val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
                }
                ReportPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                ReportPeriod.YEAR -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                ReportPeriod.ALL -> Unit
            }
        }

    private fun Calendar.moveOnePeriod(period: ReportPeriod, amount: Int) {
        when (period) {
            ReportPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, amount)
            ReportPeriod.MONTH -> add(Calendar.MONTH, amount)
            ReportPeriod.YEAR -> add(Calendar.YEAR, amount)
            ReportPeriod.ALL -> Unit
        }
    }
}
