/**
 * Формирование HTML-отчётов аналитики завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import ru.golpom.atmosphere.data.local.model.StudentSubjectCell
import ru.golpom.atmosphere.ui.deputy.ClassAnalytics
import ru.golpom.atmosphere.ui.deputy.DeputyDashboardSnapshot
import ru.golpom.atmosphere.ui.deputy.StudentAnalytics
import ru.golpom.atmosphere.ui.export.ExportPayload

object DeputyAnalyticsReportExporter {

    fun exportStudent(analytics: StudentAnalytics): ExportPayload {
        val html = DeputyStudentReportHtml.render(analytics, ReportRenderMode.SCREEN)
        val pdfHtml = DeputyStudentReportHtml.render(analytics, ReportRenderMode.PDF)
        return ExportPayload(
            fileName = DeputyStudentReportHtml.fileName(analytics),
            utf8Bytes = html.toByteArray(Charsets.UTF_8),
            pdfUtf8Bytes = pdfHtml.toByteArray(Charsets.UTF_8),
            mimeType = DeputyAnalyticsHtml.MIME_TYPE,
            recordCount = analytics.eventCount,
            periodLabel = analytics.periodLabel,
        )
    }

    fun exportClass(
        analytics: ClassAnalytics,
        heatmap: List<StudentSubjectCell>,
        subjectLabels: Map<String, String>,
    ): ExportPayload {
        val html = DeputyClassReportHtml.render(analytics, heatmap, subjectLabels, ReportRenderMode.SCREEN)
        val pdfHtml = DeputyClassReportHtml.render(analytics, heatmap, subjectLabels, ReportRenderMode.PDF)
        return ExportPayload(
            fileName = DeputyClassReportHtml.fileName(analytics),
            utf8Bytes = html.toByteArray(Charsets.UTF_8),
            pdfUtf8Bytes = pdfHtml.toByteArray(Charsets.UTF_8),
            mimeType = DeputyAnalyticsHtml.MIME_TYPE,
            recordCount = analytics.eventCount,
            periodLabel = analytics.periodLabel,
        )
    }

    fun exportSchool(snapshot: DeputyDashboardSnapshot, periodLabel: String): ExportPayload {
        val html = DeputySchoolReportHtml.render(snapshot, periodLabel, ReportRenderMode.SCREEN)
        val pdfHtml = DeputySchoolReportHtml.render(snapshot, periodLabel, ReportRenderMode.PDF)
        return ExportPayload(
            fileName = DeputySchoolReportHtml.fileName(periodLabel),
            utf8Bytes = html.toByteArray(Charsets.UTF_8),
            pdfUtf8Bytes = pdfHtml.toByteArray(Charsets.UTF_8),
            mimeType = DeputyAnalyticsHtml.MIME_TYPE,
            recordCount = snapshot.summary.totalEvents,
            periodLabel = periodLabel,
        )
    }
}
