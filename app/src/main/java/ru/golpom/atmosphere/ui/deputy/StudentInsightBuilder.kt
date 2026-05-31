/**
 * Формирование инсайтов по ученику.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.domain.WeekdayLabelsRu
import ru.golpom.atmosphere.domain.behaviorTypeLabelRu
import ru.golpom.atmosphere.ui.lesson.parseSubjectLabel
import ru.golpom.atmosphere.ui.lesson.subjectTitleGenitive
import ru.golpom.atmosphere.ui.lesson.subjectTitlePrepositional

private val zone = ZoneId.systemDefault()
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val weekFmt = DateTimeFormatter.ofPattern("dd.MM")

object StudentInsightBuilder {

    fun build(logs: List<BehaviorLogEntity>, dailyScores: List<DailyScore>): StudentInsightReport {
        val negatives = logs.filter { it.scoreImpact < 0 }
        val positives = logs.filter { it.scoreImpact > 0 }

        val weekdayPatterns = buildWeekdayPatterns(logs)
        val weeklyLoads = buildWeeklyLoads(logs)
        val subjectProblems = buildSubjectProblems(negatives)
        val subjectStrengths = buildSubjectStrengths(positives)
        val talkPoints = buildTalkPoints(negatives, positives, weekdayPatterns, subjectProblems, subjectStrengths)
        val dayDetails = buildDayDetails(logs)

        return StudentInsightReport(
            talkPoints = talkPoints,
            mainConcern = talkPoints.firstOrNull(),
            mainStrength = buildMainStrength(positives, subjectStrengths),
            weekdayPatterns = weekdayPatterns,
            weeklyLoads = weeklyLoads,
            subjectProblems = subjectProblems,
            subjectStrengths = subjectStrengths,
            dayDetails = dayDetails,
        )
    }

    private fun logDate(log: BehaviorLogEntity) =
        Instant.ofEpochMilli(log.timestamp).atZone(zone).toLocalDate()

    private fun logDow(log: BehaviorLogEntity) = logDate(log).dayOfWeek.value

    private fun buildWeekdayPatterns(logs: List<BehaviorLogEntity>): List<StudentWeekdayPattern> {
        val grouped = logs.groupBy { logDow(it) }
        return (1..5).map { dow ->
            val dayLogs = grouped[dow].orEmpty()
            val neg = dayLogs.filter { it.scoreImpact < 0 }
            StudentWeekdayPattern(
                dayOfWeek = dow,
                dayLabel = WeekdayLabelsRu.short(dow),
                dayLabelLong = WeekdayLabelsRu.nominative(dow),
                lateCount = neg.count { it.behaviorType == "late" },
                unpreparedCount = neg.count { it.behaviorType == "unprepared" },
                disruptionCount = neg.count { it.behaviorType == "disruption" },
                gadgetCount = neg.count { it.behaviorType == "gadget" },
                fightCount = neg.count { it.behaviorType in setOf("fight", "profanity") },
                positiveCount = dayLogs.count { it.scoreImpact > 0 },
                totalNegative = neg.size,
            )
        }.filter { it.hasActivity }
    }

    private fun buildWeeklyLoads(logs: List<BehaviorLogEntity>): List<StudentWeekLoad> {
        if (logs.isEmpty()) return emptyList()
        return logs
            .groupBy {
                logDate(it).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toEpochDay()
            }
            .map { (weekStart, weekLogs) ->
                val monday = java.time.LocalDate.ofEpochDay(weekStart)
                val neg = weekLogs.count { it.scoreImpact < 0 }
                val pos = weekLogs.count { it.scoreImpact > 0 }
                StudentWeekLoad(
                    weekStartEpochDay = weekStart,
                    weekLabel = weekFmt.format(monday),
                    violationCount = neg,
                    praiseCount = pos,
                    totalScore = weekLogs.sumOf { it.scoreImpact },
                )
            }
            .sortedBy { it.weekStartEpochDay }
            .takeLast(12)
    }

    private fun behaviorSummary(counts: Map<String, Int>): String =
        counts.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { (type, n) ->
                val word = when {
                    n % 10 == 1 && n % 100 != 11 -> "раз"
                    n % 10 in 2..4 && n % 100 !in 12..14 -> "раза"
                    else -> "раз"
                }
                "${behaviorTypeLabelRu(type)} — $n $word"
            }

    private fun buildSubjectProblems(negatives: List<BehaviorLogEntity>): List<StudentSubjectInsight> {
        return negatives
            .groupBy { it.subjectKey }
            .map { (key, entries) ->
                val label = parseSubjectLabel(key)
                val byType = entries.groupingBy { it.behaviorType }.eachCount()
                StudentSubjectInsight(
                    subjectTitle = label.title,
                    subjectContext = label.context,
                    summary = behaviorSummary(byType),
                    eventCount = entries.size,
                )
            }
            .sortedByDescending { it.eventCount }
    }

    private fun buildSubjectStrengths(positives: List<BehaviorLogEntity>): List<StudentSubjectInsight> {
        return positives
            .groupBy { it.subjectKey }
            .map { (key, entries) ->
                val label = parseSubjectLabel(key)
                val byType = entries.groupingBy { it.behaviorType }.eachCount()
                StudentSubjectInsight(
                    subjectTitle = label.title,
                    subjectContext = label.context,
                    summary = behaviorSummary(byType),
                    eventCount = entries.size,
                )
            }
            .sortedByDescending { it.eventCount }
    }

    private fun buildTalkPoints(
        negatives: List<BehaviorLogEntity>,
        positives: List<BehaviorLogEntity>,
        weekdays: List<StudentWeekdayPattern>,
        problems: List<StudentSubjectInsight>,
        strengths: List<StudentSubjectInsight>,
    ): List<String> {
        val points = mutableListOf<String>()

        negatives.filter { it.behaviorType == "late" }
            .groupBy { logDow(it) }
            .maxByOrNull { it.value.size }
            ?.let { (dow, list) ->
                val subjects = list.groupBy { it.subjectKey }
                    .map { parseSubjectLabel(it.key).title to it.value.size }
                    .sortedByDescending { it.second }
                    .take(2)
                    .joinToString(", ") { "${it.first} (${it.second})" }
                points.add(
                    "Опоздания: чаще всего ${WeekdayLabelsRu.onDays(dow)}" +
                        if (subjects.isNotBlank()) " — уроки: $subjects." else ".",
                )
            }

        negatives.filter { it.behaviorType == "unprepared" }
            .groupBy { it.subjectKey }
            .maxByOrNull { it.value.size }
            ?.let { (key, list) ->
                val title = parseSubjectLabel(key).title
                points.add(
                    "Не готов к урокам: чаще всего по ${subjectTitlePrepositional(title)} " +
                        "(${list.size} отметок). Обсудить домашние задания и подготовку.",
                )
            }

        negatives.filter { it.behaviorType == "disruption" }
            .groupBy { it.subjectKey }
            .maxByOrNull { it.value.size }
            ?.let { (key, list) ->
                val subj = subjectTitleGenitive(parseSubjectLabel(key).title)
                points.add("Срывы дисциплины: чаще на уроках $subj (${list.size}).")
            }

        negatives.filter { it.behaviorType == "gadget" }
            .groupingBy { it.subjectKey }
            .eachCount()
            .maxByOrNull { it.value }
            ?.let { (key, n) ->
                points.add(
                    "Телефон на уроке: $n раз, в том числе на ${subjectTitlePrepositional(parseSubjectLabel(key).title)}.",
                )
            }

        problems.firstOrNull()?.let { p ->
            if (points.none { it.contains(p.subjectTitle) }) {
                points.add("Слабое место по предмету: ${p.subjectTitle} — ${p.summary}.")
            }
        }

        strengths.firstOrNull()?.let { s ->
            points.add(
                "Сильная сторона: на ${subjectTitlePrepositional(s.subjectTitle)} — ${s.summary}. " +
                    "Можно опереться на этот предмет в разговоре.",
            )
        }

        weekdays.filter { it.totalNegative >= 2 }
            .maxByOrNull { it.totalNegative }
            ?.let { w ->
                if (points.none { it.contains(w.dayLabelLong) }) {
                    points.add(
                        "Напряжённый день недели — ${WeekdayLabelsRu.titled(w.dayOfWeek)}: " +
                            "${w.totalNegative} нарушений за период.",
                    )
                }
            }

        if (points.isEmpty() && negatives.isEmpty() && positives.isNotEmpty()) {
            points.add("Серьёзных нарушений за период нет. Обсудить закрепление положительного поведения.")
        }
        if (points.isEmpty() && negatives.isNotEmpty()) {
            points.add("Есть нарушения (${negatives.size}) — разобрать конкретные дни в календаре ниже.")
        }

        return points.take(6)
    }

    private fun buildMainStrength(
        positives: List<BehaviorLogEntity>,
        strengths: List<StudentSubjectInsight>,
    ): String? {
        val top = strengths.firstOrNull() ?: return null
        if (positives.isEmpty()) return null
        return "Прилежность: на ${subjectTitlePrepositional(top.subjectTitle)} — ${top.summary}."
    }

    private fun buildDayDetails(logs: List<BehaviorLogEntity>): Map<Long, StudentDayDetail> =
        logs.groupBy { logDate(it).toEpochDay() }
            .mapValues { (epochDay, dayLogs) ->
                val date = java.time.LocalDate.ofEpochDay(epochDay)
                val sorted = dayLogs.sortedBy { it.timestamp }
                val events = sorted.map { log ->
                    val subj = parseSubjectLabel(log.subjectKey)
                    StudentDayEvent(
                        timeLabel = timeFmt.format(Instant.ofEpochMilli(log.timestamp)),
                        subjectTitle = subj.title,
                        subjectContext = subj.context,
                        behaviorLabel = behaviorTypeLabelRu(log.behaviorType),
                        scoreImpact = log.scoreImpact,
                        isPositive = log.scoreImpact > 0,
                        studentName = null,
                    )
                }
                val neg = dayLogs.count { it.scoreImpact < 0 }
                val pos = dayLogs.count { it.scoreImpact > 0 }
                val summary = when {
                    neg == 0 && pos > 0 -> "$pos поощрений за день — спокойный день."
                    neg > 0 && pos == 0 -> "$neg нарушений — день под вниманием."
                    else -> "$pos поощрений, $neg нарушений."
                }
                StudentDayDetail(
                    epochDay = epochDay,
                    dateLabel = dateFmt.format(date),
                    weekdayLabel = WeekdayLabelsRu.titled(date.dayOfWeek.value),
                    summary = summary,
                    events = events,
                )
            }
}
