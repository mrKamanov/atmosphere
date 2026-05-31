/**
 * Диапазон дат периода для аналитики завуча.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DeputyPeriodRange {
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun calc(cfg: PeriodConfig, now: ZonedDateTime = ZonedDateTime.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return when (cfg.type) {
            PeriodType.WEEK -> {
                val start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zone)
                start.toInstant().toEpochMilli() to now.toInstant().toEpochMilli()
            }
            PeriodType.MONTH -> {
                val start = now.minusDays(30).toLocalDate().atStartOfDay(zone)
                start.toInstant().toEpochMilli() to now.toInstant().toEpochMilli()
            }
            PeriodType.YEAR -> {
                val d = now.toLocalDate()
                val yearStart = if (d.monthValue >= 9) d.year else d.year - 1
                val start = java.time.LocalDate.of(yearStart, 9, 1).atStartOfDay(zone)
                val end = java.time.LocalDate.of(yearStart + 1, 5, 31).atTime(23, 59, 59).atZone(zone)
                start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
            }
            PeriodType.CUSTOM -> {
                val start = cfg.fromDate?.let { Instant.ofEpochMilli(it).atZone(zone) }
                    ?: now.minusDays(30).toLocalDate().atStartOfDay(zone)
                val end = cfg.toDate?.let { Instant.ofEpochMilli(it).atZone(zone) } ?: now
                start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
            }
        }
    }

    fun label(cfg: PeriodConfig, now: ZonedDateTime = ZonedDateTime.now()): String {
        val zone = ZoneId.systemDefault()
        return when (cfg.type) {
            PeriodType.WEEK -> "Текущая неделя"
            PeriodType.MONTH -> "Последние 30 дней"
            PeriodType.YEAR -> "Учебный год"
            PeriodType.CUSTOM -> {
                val (from, to) = calc(cfg, now)
                val fromDate = Instant.ofEpochMilli(from).atZone(zone).toLocalDate()
                val toDate = Instant.ofEpochMilli(to).atZone(zone).toLocalDate()
                "${dateFmt.format(fromDate)} — ${dateFmt.format(toDate)}"
            }
        }
    }
}
