/**
 * Расчёт границ периода выгрузки (from inclusive, to exclusive — как в Room-запросах).
 */
package ru.golpom.atmosphere.domain.export

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object ExportPeriodCalculator {

    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val monthFmt = DateTimeFormatter.ofPattern("MM.yyyy")

    fun calc(
        selection: ExportPeriodSelection,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
    ): Pair<Long, Long> {
        val zone = now.zone
        return when (selection.kind) {
            ExportPeriodKind.DAY -> dayRange(selection, now, zone)
            ExportPeriodKind.WEEK -> weekRange(selection, now, zone)
            ExportPeriodKind.MONTH -> monthRange(selection, now, zone)
            ExportPeriodKind.SCHOOL_YEAR -> schoolYearRange(selection, now, zone)
        }
    }

    fun label(selection: ExportPeriodSelection, now: ZonedDateTime = ZonedDateTime.now()): String {
        val zone = now.zone
        return when (selection.kind) {
            ExportPeriodKind.DAY -> {
                if (selection.useCurrent) "Сегодня" else {
                    val d = anchorDate(selection, now, zone)
                    dateFmt.format(d)
                }
            }
            ExportPeriodKind.WEEK -> {
                if (selection.useCurrent) "Текущая неделя (пн–сб)" else {
                    val monday = weekMonday(selection, now, zone)
                    val saturday = monday.plusDays(5)
                    "${dateFmt.format(monday)} — ${dateFmt.format(saturday)}"
                }
            }
            ExportPeriodKind.MONTH -> {
                if (selection.useCurrent) "Текущий месяц до сегодня" else {
                    val first = monthFirstDay(selection, now, zone)
                    monthFmt.format(first)
                }
            }
            ExportPeriodKind.SCHOOL_YEAR -> {
                val (start, endExclusive) = schoolYearRange(selection, now, zone)
                val startDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
                val endDate = Instant.ofEpochMilli(endExclusive).atZone(zone).toLocalDate().minusDays(1)
                if (selection.useCurrent) {
                    "С 01.09.${startDate.year} по ${dateFmt.format(endDate)}"
                } else {
                    "01.09.${startDate.year} — ${dateFmt.format(endDate)}"
                }
            }
        }
    }

    /** Короткая метка для имени файла. */
    fun periodTag(selection: ExportPeriodSelection, now: ZonedDateTime = ZonedDateTime.now()): String {
        val zone = now.zone
        return when (selection.kind) {
            ExportPeriodKind.DAY -> {
                val d = if (selection.useCurrent) now.toLocalDate() else anchorDate(selection, now, zone)
                "day-${d}"
            }
            ExportPeriodKind.WEEK -> {
                val monday = if (selection.useCurrent) {
                    now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                } else {
                    weekMonday(selection, now, zone)
                }
                "week-$monday"
            }
            ExportPeriodKind.MONTH -> {
                val first = if (selection.useCurrent) now.toLocalDate().withDayOfMonth(1)
                else monthFirstDay(selection, now, zone)
                "month-${first.year}-${first.monthValue.toString().padStart(2, '0')}"
            }
            ExportPeriodKind.SCHOOL_YEAR -> {
                val startYear = schoolYearStartYear(
                    if (selection.useCurrent) now.toLocalDate()
                    else anchorDate(selection, now, zone),
                )
                "year-$startYear-${startYear + 1}"
            }
        }
    }

    private fun dayRange(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val day = if (selection.useCurrent) now.toLocalDate() else anchorDate(selection, now, zone)
        return day.atStartOfDay(zone).toInstant().toEpochMilli() to
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** Понедельник 00:00 — воскресенье 00:00 (сб включительно). */
    private fun weekRange(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val monday = if (selection.useCurrent) {
            now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
        } else {
            weekMonday(selection, now, zone)
        }
        val end = monday.plusDays(6).atStartOfDay(zone).toInstant().toEpochMilli() // вс 00:00
        return monday.atStartOfDay(zone).toInstant().toEpochMilli() to end
    }

    private fun monthRange(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val first = if (selection.useCurrent) now.toLocalDate().withDayOfMonth(1)
        else monthFirstDay(selection, now, zone)
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = if (selection.useCurrent) {
            now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            val lastDay = first.withDayOfMonth(first.lengthOfMonth())
            val today = now.toLocalDate()
            val effectiveEnd = if (first.year == today.year && first.month == today.month) {
                today
            } else {
                lastDay
            }
            effectiveEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        return start to end
    }

    private fun schoolYearRange(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val ref = if (selection.useCurrent) now.toLocalDate() else anchorDate(selection, now, zone)
        val startYear = schoolYearStartYear(ref)
        val start = LocalDate.of(startYear, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = if (selection.useCurrent) {
            now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            ref.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        return start to end
    }

    private fun anchorDate(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): LocalDate = selection.anchorDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
    } ?: now.toLocalDate()

    private fun weekMonday(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): LocalDate {
        val ref = anchorDate(selection, now, zone)
        return ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    private fun monthFirstDay(
        selection: ExportPeriodSelection,
        now: ZonedDateTime,
        zone: ZoneId,
    ): LocalDate = anchorDate(selection, now, zone).withDayOfMonth(1)

    private fun schoolYearStartYear(ref: LocalDate): Int =
        if (ref.monthValue >= 9) ref.year else ref.year - 1
}
