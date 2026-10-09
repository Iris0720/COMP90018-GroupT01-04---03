package com.example.comp90018.data.repository

import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.ReportPeriod
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HistoryWindowCalculatorTest {
    @Test
    fun week_startsOnMondayInRequestedTimeZone() {
        val zone = TimeZone.getTimeZone("Australia/Melbourne")
        val reference = calendar(zone, 2026, Calendar.OCTOBER, 3, 15).timeInMillis

        val window = HistoryWindowCalculator.calculate(
            HistoryQuery(ReportPeriod.WEEK, reference, zone.id)
        )

        assertEquals(calendar(zone, 2026, Calendar.SEPTEMBER, 28, 0).timeInMillis, window.startEpochMillis)
        assertEquals(calendar(zone, 2026, Calendar.OCTOBER, 5, 0).timeInMillis, window.endEpochMillis)
    }

    @Test
    fun month_handlesDaylightSavingBoundary() {
        val zone = TimeZone.getTimeZone("Australia/Melbourne")
        val reference = calendar(zone, 2026, Calendar.OCTOBER, 15, 12).timeInMillis

        val window = HistoryWindowCalculator.calculate(
            HistoryQuery(ReportPeriod.MONTH, reference, zone.id)
        )

        assertEquals(calendar(zone, 2026, Calendar.OCTOBER, 1, 0).timeInMillis, window.startEpochMillis)
        assertEquals(calendar(zone, 2026, Calendar.NOVEMBER, 1, 0).timeInMillis, window.endEpochMillis)
    }

    @Test
    fun unknownTimeZone_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HistoryWindowCalculator.calculate(
                HistoryQuery(ReportPeriod.WEEK, 1_000, "Not/A_Time_Zone")
            )
        }
    }

    @Test
    fun year_startsOnJanuaryFirstInRequestedTimeZone() {
        val zone = TimeZone.getTimeZone("Australia/Melbourne")
        val reference = calendar(zone, 2025, Calendar.OCTOBER, 5, 12).timeInMillis

        val window = HistoryWindowCalculator.calculate(
            HistoryQuery(ReportPeriod.YEAR, reference, zone.id)
        )

        assertEquals(
            calendar(zone, 2025, Calendar.JANUARY, 1, 0).timeInMillis,
            window.startEpochMillis
        )
        assertEquals(
            calendar(zone, 2026, Calendar.JANUARY, 1, 0).timeInMillis,
            window.endEpochMillis
        )
    }

    private fun calendar(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int
    ): Calendar = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }
}
