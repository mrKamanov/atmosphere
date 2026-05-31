/**
 * Формирование инсайтов по классу для отчёта завуча.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.domain.WeekdayLabelsRu
import ru.golpom.atmosphere.domain.behaviorTypeLabelRu
import ru.golpom.atmosphere.ui.lesson.parseSubjectLabel
import ru.golpom.atmosphere.ui.lesson.subjectTitleGenitive
import ru.golpom.atmosphere.ui.lesson.subjectTitlePrepositional

private val zone = ZoneId.systemDefault()
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

object ClassInsightBuilder {

    fun build(
        logs: List<BehaviorLogEntity>,
        dailyScores: List<DailyScore>,
        students: List<StudentEntity>,
    ): InsightReport {
        val names = students.associate { it.studentId to "${it.lastName} ${it.firstName}" }
        val base = StudentInsightBuilder.build(logs, dailyScores)
        return base.copy(
            talkPoints = buildClassTalkPoints(logs, names, base),
            dayDetails = buildClassDayDetails(logs, names),
        )
    }

    private fun logDate(log: BehaviorLogEntity) =
        Instant.ofEpochMilli(log.timestamp).atZone(zone).toLocalDate()

    private fun logDow(log: BehaviorLogEntity) = logDate(log).dayOfWeek.value

    private fun topStudentsByBehavior(
        logs: List<BehaviorLogEntity>,
        behaviorType: String,
        names: Map<String, String>,
        limit: Int = 3,
    ): String =
        logs.filter { it.behaviorType == behaviorType && it.scoreImpact < 0 }
            .groupingBy { it.studentId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .joinToString(", ") { (sid, n) ->
                "${names[sid] ?: "ученик"} ($n)"
            }

    private fun buildClassTalkPoints(
        logs: List<BehaviorLogEntity>,
        names: Map<String, String>,
        base: InsightReport,
    ): List<String> {
        val points = mutableListOf<String>()
        val negatives = logs.filter { it.scoreImpact < 0 }
        val positives = logs.filter { it.scoreImpact > 0 }

        val lateStudents = topStudentsByBehavior(logs, "late", names)
        if (lateStudents.isNotBlank()) {
            negatives.filter { it.behaviorType == "late" }
                .groupBy { logDow(it) }
                .maxByOrNull { it.value.size }
                ?.let { (dow, _) ->
                    points.add(
                        "Опоздания в классе: $lateStudents. Чаще всего ${WeekdayLabelsRu.onDays(dow)}. " +
                            "Пригласить на беседу, связаться с родителями.",
                    )
                }
        }

        val unpreparedStudents = topStudentsByBehavior(logs, "unprepared", names)
        negatives.filter { it.behaviorType == "unprepared" }
            .groupBy { it.subjectKey }
            .maxByOrNull { it.value.size }
            ?.let { (key, list) ->
                val subj = subjectTitlePrepositional(parseSubjectLabel(key).title)
                points.add(
                    "Не готовы к урокам: чаще по $subj (${list.size} отметок по классу)." +
                        if (unpreparedStudents.isNotBlank()) " Лидеры: $unpreparedStudents." else "",
                )
            }

        negatives.filter { it.behaviorType == "disruption" }
            .groupBy { it.subjectKey }
            .maxByOrNull { it.value.size }
            ?.let { (key, list) ->
                val disruptors = topStudentsByBehavior(
                    logs.filter { it.subjectKey == key },
                    "disruption",
                    names,
                    2,
                )
                points.add(
                    "Срывы дисциплины: ${subjectTitleGenitive(parseSubjectLabel(key).title)} (${list.size})." +
                        if (disruptors.isNotBlank()) " Чаще: $disruptors." else "",
                )
            }

        negatives.groupingBy { it.studentId }.eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(2)
            .forEach { (sid, n) ->
                if (n >= 3) {
                    points.add(
                        "Индивидуально: ${names[sid] ?: "ученик"} — $n нарушений за период. " +
                            "Запланировать беседу с родителями.",
                    )
                }
            }

        base.subjectProblems.firstOrNull()?.let { p ->
            if (points.none { it.contains(p.subjectTitle) }) {
                points.add("Слабый предмет у коллектива: ${p.subjectTitle} — ${p.summary}.")
            }
        }

        positives.groupingBy { it.studentId }.eachCount()
            .entries
            .sortedByDescending { it.value }
            .firstOrNull()
            ?.takeIf { it.value >= 3 }
            ?.let { (sid, n) ->
                points.add(
                    "Опора класса: ${names[sid] ?: "ученик"} — $n поощрений. Можно выделить на линейке.",
                )
            }

        base.subjectStrengths.firstOrNull()?.let { s ->
            points.add("Коллектив силён на ${subjectTitlePrepositional(s.subjectTitle)} — ${s.summary}.")
        }

        base.weekdayPatterns.filter { it.totalNegative >= 3 }
            .maxByOrNull { it.totalNegative }
            ?.let { w ->
                points.add(
                    "Напряжённый день — ${WeekdayLabelsRu.titled(w.dayOfWeek)}: ${w.totalNegative} нарушений. " +
                        "Имеет смысл усилить контроль ${WeekdayLabelsRu.inDay(w.dayOfWeek)}.",
                )
            }

        if (points.isEmpty()) {
            points.addAll(base.talkPoints.map { it.replace("ученик", "класс").replace("с учеником", "с коллективом") })
        }

        return points.take(7)
    }

    private fun buildClassDayDetails(
        logs: List<BehaviorLogEntity>,
        names: Map<String, String>,
    ): Map<Long, StudentDayDetail> =
        logs.groupBy { logDate(it).toEpochDay() }
            .mapValues { (epochDay, dayLogs) ->
                val date = java.time.LocalDate.ofEpochDay(epochDay)
                val sorted = dayLogs.sortedByDescending { abs(it.scoreImpact) }.take(25)
                val events = sorted.map { log ->
                    val subj = parseSubjectLabel(log.subjectKey)
                    StudentDayEvent(
                        timeLabel = timeFmt.format(Instant.ofEpochMilli(log.timestamp)),
                        subjectTitle = subj.title,
                        subjectContext = subj.context,
                        behaviorLabel = behaviorTypeLabelRu(log.behaviorType),
                        scoreImpact = log.scoreImpact,
                        isPositive = log.scoreImpact > 0,
                        studentName = names[log.studentId],
                    )
                }
                val neg = dayLogs.count { it.scoreImpact < 0 }
                val pos = dayLogs.count { it.scoreImpact > 0 }
                val uniqueStudents = dayLogs.map { it.studentId }.distinct().size
                val summary = when {
                    neg == 0 && pos > 0 -> "$pos поощрений, $uniqueStudents учеников — хороший день."
                    neg > 0 && pos == 0 -> "$neg нарушений у $uniqueStudents учеников — день под контролем."
                    else -> "$pos поощрений, $neg нарушений ($uniqueStudents учеников)."
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

private fun abs(v: Int) = kotlin.math.abs(v)
