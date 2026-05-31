/**
 * Общая HTML-вёрстка отчётов аналитики завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal object DeputyAnalyticsHtml {
    const val MIME_TYPE = "text/html; charset=utf-8"

    /** Предметов в одной части heatmap — чтобы таблица помещалась на лист A4 при печати в PDF. */
    const val HEATMAP_SUBJECTS_PER_CHUNK = 5

    private val generatedDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

    fun document(title: String, body: String, mode: ReportRenderMode = ReportRenderMode.SCREEN): String = """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>${escape(title)}</title>
          <style>
            * { box-sizing: border-box; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              color: #1e2d2a;
              background: #f2f6f4;
              margin: 0;
              padding: 24px 16px 48px;
              line-height: 1.45;
              font-size: 14px;
            }
            .page {
              max-width: 720px;
              margin: 0 auto;
              background: #fff;
              border-radius: 16px;
              padding: 0 0 28px;
              box-shadow: 0 2px 12px rgba(61,122,106,0.08);
              overflow: hidden;
            }
            .hero {
              background: linear-gradient(165deg, #3d7a6a 0%, #5ba68a 45%, #8ecdb8 100%);
              color: #f8fffc;
              padding: 28px 24px 24px;
              margin-bottom: 8px;
            }
            .hero-brand {
              font-size: 11px;
              letter-spacing: 0.12em;
              text-transform: uppercase;
              opacity: 0.85;
              margin-bottom: 8px;
            }
            .hero h1 {
              font-size: 24px;
              font-weight: 600;
              margin: 0 0 6px;
              color: #fff;
            }
            .hero .meta { color: rgba(248,255,252,0.88); margin: 0; font-size: 13px; }
            .content { padding: 8px 24px 0; }
            h2 {
              font-size: 15px;
              font-weight: 600;
              margin: 28px 0 12px;
              padding-bottom: 6px;
              border-bottom: 2px solid #5ba68a;
              color: #1e2d2a;
            }
            .stats {
              display: flex;
              flex-wrap: wrap;
              gap: 10px;
              margin: 16px 0;
            }
            .stat {
              flex: 1 1 110px;
              background: #f2f6f4;
              border-radius: 12px;
              padding: 12px 14px;
              min-width: 96px;
              border-left: 4px solid #5ba68a;
            }
            .stat.pos { border-left-color: #3d9a62; background: #e3f5ea; }
            .stat.neg { border-left-color: #c95555; background: #fceaea; }
            .stat-label { font-size: 10px; color: #8a9e97; text-transform: uppercase; letter-spacing: 0.04em; }
            .stat-value { font-size: 20px; font-weight: 600; margin-top: 4px; }
            .pos { color: #3d9a62; }
            .neg { color: #c95555; }
            .neu { color: #4a5f5a; }
            ul { margin: 8px 0; padding-left: 20px; }
            li { margin: 6px 0; }
            .talk-list {
              list-style: none;
              padding: 0;
              margin: 12px 0;
            }
            .talk-list li {
              background: #f2f6f4;
              border-radius: 10px;
              padding: 10px 12px 10px 28px;
              margin: 8px 0;
              position: relative;
            }
            .talk-list li::before {
              content: "";
              position: absolute;
              left: 12px;
              top: 15px;
              width: 8px;
              height: 8px;
              border-radius: 50%;
              background: #5ba68a;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              font-size: 13px;
              margin: 12px 0;
            }
            th, td {
              border: 1px solid #e0eae6;
              padding: 8px 10px;
              text-align: left;
            }
            th { background: #e8f0ed; font-weight: 600; }
            tr:nth-child(even) td { background: #fafbfb; }
            table.heatmap {
              font-size: 11px;
              table-layout: fixed;
            }
            table.heatmap th,
            table.heatmap td.name-col {
              background: #e8f0ed;
            }
            table.heatmap .name-col {
              width: 30%;
              font-weight: 600;
              text-align: left;
              word-wrap: break-word;
            }
            table.heatmap th {
              font-size: 10px;
              padding: 6px 4px;
              text-align: center;
              vertical-align: bottom;
              word-wrap: break-word;
              hyphens: auto;
            }
            table.heatmap td {
              padding: 7px 4px;
              text-align: center;
              font-weight: 600;
              font-size: 11px;
            }
            .chunk-label {
              font-size: 12px;
              color: #8a9e97;
              margin: 16px 0 8px;
              font-weight: 500;
            }
            .table-chunk {
              margin-bottom: 20px;
              page-break-inside: avoid;
            }
            .heatmap-legend {
              display: flex;
              flex-wrap: wrap;
              gap: 12px;
              font-size: 11px;
              color: #8a9e97;
              margin: 8px 0 12px;
            }
            .heatmap-legend span { display: inline-flex; align-items: center; gap: 6px; }
            .swatch {
              display: inline-block;
              width: 14px;
              height: 14px;
              border-radius: 4px;
              border: 1px solid #e0eae6;
            }
            .card {
              background: #f2f6f4;
              border-radius: 10px;
              padding: 12px 14px;
              margin: 8px 0;
            }
            .card-accent {
              border-left: 4px solid #5ba68a;
              background: #fff;
              border: 1px solid #e0eae6;
              border-left-width: 4px;
            }
            .card-title { font-weight: 600; font-size: 14px; }
            .card-sub { font-size: 11px; color: #8a9e97; }
            .card-body { font-size: 13px; margin-top: 6px; }
            .chart-box {
              background: #f2f6f4;
              border-radius: 12px;
              padding: 14px 12px;
              margin: 12px 0;
              border: 1px solid #e0eae6;
            }
            .chart-caption {
              font-size: 11px;
              color: #8a9e97;
              margin-bottom: 8px;
            }
            .chart-scroll {
              overflow-x: auto;
              -webkit-overflow-scrolling: touch;
              padding-bottom: 4px;
            }
            .chart-svg {
              width: 100%;
              height: auto;
              display: block;
            }
            .chart-svg-fixed {
              width: auto;
              height: auto;
              display: block;
              max-width: none;
            }
            .bar-chart-scroll {
              overflow-x: auto;
              -webkit-overflow-scrolling: touch;
              margin: 0 -4px;
              padding-bottom: 4px;
            }
            .bar-chart {
              display: flex;
              align-items: flex-end;
              gap: 6px;
              min-height: 148px;
              padding: 4px 4px 0;
            }
            .bar-chart-pdf {
              display: flex;
              flex-direction: column;
              gap: 14px;
            }
            .bar-chart-pdf-row {
              max-width: 100%;
            }
            .bar-slot {
              flex: 0 0 auto;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: flex-end;
            }
            .bar-track {
              width: 100%;
              display: flex;
              align-items: flex-end;
              justify-content: center;
            }
            .bar-fill {
              width: 78%;
              border-radius: 4px 4px 0 0;
              min-height: 3px;
            }
            .bar-fill.pos { background: #3d9a62; }
            .bar-fill.neg { background: #c95555; }
            .bar-fill.neu { background: #e0eae6; }
            .bar-top-label {
              font-size: 10px;
              font-weight: 600;
              margin-bottom: 4px;
              white-space: nowrap;
              min-height: 14px;
            }
            .bar-x-label {
              font-size: 9px;
              color: #8a9e97;
              margin-top: 6px;
              text-align: center;
              line-height: 1.25;
              word-break: break-word;
              hyphens: auto;
            }
            .parallel-card {
              background: #f0f3f2;
              border-radius: 12px;
              padding: 14px;
              margin: 10px 0;
            }
            .parallel-card-head {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 10px;
            }
            .parallel-card-title { font-size: 13px; font-weight: 600; }
            .parallel-card-total { font-size: 15px; font-weight: 600; }
            .parallel-class-row {
              display: flex;
              align-items: center;
              gap: 8px;
              padding: 4px 0;
            }
            .parallel-class-id {
              width: 44px;
              font-size: 12px;
              font-weight: 500;
              flex-shrink: 0;
            }
            .parallel-bar-track {
              flex: 1;
              height: 12px;
              background: #e0eae6;
              border-radius: 3px;
              overflow: hidden;
            }
            .parallel-bar-fill {
              height: 100%;
              border-radius: 3px;
              min-width: 4px;
            }
            .parallel-bar-fill.pos { background: #3d9a62; }
            .parallel-bar-fill.neg { background: #c95555; }
            .parallel-class-score {
              width: 40px;
              font-size: 12px;
              font-weight: 600;
              text-align: right;
              flex-shrink: 0;
            }
            .bar-label { font-size: 9px; fill: #8a9e97; }
            .bar-label-pos { font-size: 9px; fill: #3d9a62; font-weight: 600; }
            .bar-value { font-size: 9px; fill: #1e2d2a; font-weight: 600; }
            .bar-value-in { font-size: 9px; fill: #fff; font-weight: 600; }
            .donut-main { font-size: 18px; font-weight: 700; fill: #1e2d2a; }
            .donut-sub { font-size: 9px; fill: #8a9e97; }
            .legend { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; font-size: 12px; }
            .legend-item { display: inline-flex; align-items: center; gap: 6px; }
            .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
            .dot.pos { background: #3d9a62; }
            .dot.neg { background: #c95555; }
            .rank-label { font-size: 11px; fill: #1e2d2a; font-weight: 600; }
            .rank-value { font-size: 11px; fill: #4a5f5a; font-weight: 600; }
            .two-col {
              display: flex;
              flex-wrap: wrap;
              gap: 16px;
              align-items: flex-start;
            }
            .two-col > * { flex: 1 1 240px; }
            .title-page {
              min-height: 520px;
              background: linear-gradient(165deg, #3d7a6a 0%, #5ba68a 40%, #8ecdb8 75%, #b8e6d4 100%);
              color: #f8fffc;
              display: flex;
              flex-direction: column;
              justify-content: center;
              align-items: center;
              text-align: center;
              padding: 48px 32px;
              position: relative;
              overflow: hidden;
            }
            .title-page::before,
            .title-page::after {
              content: "";
              position: absolute;
              border-radius: 50%;
              background: rgba(255,255,255,0.1);
            }
            .title-page::before {
              width: 280px;
              height: 280px;
              top: -80px;
              right: -60px;
            }
            .title-page::after {
              width: 180px;
              height: 180px;
              bottom: -40px;
              left: -30px;
            }
            .title-inner { position: relative; z-index: 1; max-width: 480px; }
            .title-app {
              font-size: 13px;
              letter-spacing: 0.2em;
              text-transform: uppercase;
              opacity: 0.9;
              margin-bottom: 20px;
            }
            .title-badge {
              display: inline-block;
              background: rgba(255,255,255,0.18);
              border: 1px solid rgba(255,255,255,0.35);
              border-radius: 999px;
              padding: 6px 16px;
              font-size: 12px;
              letter-spacing: 0.06em;
              margin-bottom: 24px;
            }
            .title-headline {
              font-size: 32px;
              font-weight: 700;
              margin: 0 0 12px;
              line-height: 1.2;
              color: #fff;
            }
            .title-period {
              font-size: 16px;
              opacity: 0.92;
              margin: 0 0 8px;
            }
            .title-details {
              font-size: 13px;
              opacity: 0.82;
              margin: 16px 0 0;
              line-height: 1.6;
            }
            .title-date {
              margin-top: 40px;
              font-size: 12px;
              opacity: 0.75;
            }
            .title-note {
              margin-top: 8px;
              font-size: 11px;
              opacity: 0.65;
            }
            .section-head {
              font-size: 18px;
              font-weight: 600;
              color: #3d7a6a;
              margin: 0 0 4px;
            }
            .section-meta {
              font-size: 12px;
              color: #8a9e97;
              margin: 0 0 16px;
            }
            .footer {
              margin-top: 32px;
              padding: 0 24px;
              font-size: 11px;
              color: #8a9e97;
              text-align: center;
            }
            @media print {
              body { background: #fff; padding: 0; }
              .page { box-shadow: none; border-radius: 0; max-width: none; }
              .title-page {
                min-height: 100vh;
                page-break-after: always;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
              }
              .table-chunk + .table-chunk { page-break-before: always; }
              .hero { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
              .chart-box, table.heatmap td, .bar-fill, .parallel-bar-fill { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
              .bar-chart-scroll { overflow: visible; }
              .bar-chart-pdf-row { page-break-inside: avoid; }
            }
            body.pdf-export .bar-chart-scroll { overflow: visible; }
            body.pdf-export .page { max-width: none; }
          </style>
        </head>
        <body${if (mode == ReportRenderMode.PDF) " class=\"pdf-export\"" else ""}>
          <div class="page">
            $body
            <p class="footer">Атмосфера · ${escape(generatedDateFormat.format(LocalDate.now()))}</p>
          </div>
        </body>
        </html>
    """.trimIndent().trimStart()

    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun formatSigned(v: Int): String = if (v >= 0) "+$v" else "$v"

    fun statBlock(label: String, value: String, cssClass: String = "neu"): String {
        val statCls = when (cssClass) {
            "pos" -> "stat pos"
            "neg" -> "stat neg"
            else -> "stat"
        }
        return """<div class="$statCls"><div class="stat-label">${escape(label)}</div><div class="stat-value $cssClass">${escape(value)}</div></div>"""
    }

    fun safeFilePart(raw: String): String =
        raw.trim().replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").take(48).ifBlank { "отчёт" }

    /** Титульный лист отчёта (отдельная страница при печати в PDF). */
    fun titlePage(
        level: String,
        headline: String,
        periodLabel: String,
        details: List<String> = emptyList(),
    ): String {
        val detailsHtml = if (details.isEmpty()) "" else {
            details.joinToString("<br/>") { escape(it) }.let { """<p class="title-details">$it</p>""" }
        }
        val dateLine = escape(generatedDateFormat.format(LocalDate.now()))
        return """
            <section class="title-page">
              <div class="title-inner">
                <div class="title-app">Атмосфера</div>
                <div class="title-badge">Отчёт · ${escape(level)}</div>
                <h1 class="title-headline">${escape(headline)}</h1>
                <p class="title-period">${escape(periodLabel)}</p>
                $detailsHtml
                <p class="title-date">Сформирован $dateLine</p>
                <p class="title-note">Для внутреннего использования · содержит персональные данные</p>
              </div>
            </section>
        """.trimIndent()
    }

    fun sectionIntro(headline: String, meta: String): String = """
        <p class="section-head">${escape(headline)}</p>
        <p class="section-meta">${escape(meta)}</p>
    """.trimIndent()

    fun heatmapLegend(): String = """
        <div class="heatmap-legend">
          <span><i class="swatch" style="background:#3d9a62"></i> поощрения</span>
          <span><i class="swatch" style="background:#c95555"></i> нарушения</span>
          <span><i class="swatch" style="background:#f0f3f2"></i> нет отметок</span>
        </div>
    """.trimIndent()

    fun heatmapCell(score: Int, maxAbs: Int): String {
        val bg = heatmapBackground(score, maxAbs)
        val fg = heatmapForeground(score, maxAbs)
        val label = if (score == 0) "·" else formatSigned(score)
        return """<td style="background:$bg;color:$fg">$label</td>"""
    }

    /**
     * Heatmap с разбиением по предметам: в каждой части повторяется столбец «Ученик».
     */
    fun chunkedStudentSubjectHeatmap(
        students: List<Pair<String, String>>,
        subjectKeys: List<String>,
        subjectLabels: Map<String, String>,
        scores: Map<Pair<String, String>, Int>,
        sectionTitle: String = "Ученики и предметы",
    ): String {
        if (students.isEmpty() || subjectKeys.isEmpty()) return ""
        val maxAbs = max(scores.values.maxOfOrNull { abs(it) } ?: 1, 1)
        val chunks = subjectKeys.chunked(HEATMAP_SUBJECTS_PER_CHUNK)
        val sb = StringBuilder()
        sb.append("<h2>${escape(sectionTitle)}</h2>")
        sb.append(heatmapLegend())
        chunks.forEachIndexed { idx, chunk ->
            if (chunks.size > 1) {
                sb.append(
                    """<p class="chunk-label">${escape(sectionTitle)} · часть ${idx + 1} из ${chunks.size}</p>""",
                )
            }
            sb.append("""<div class="table-chunk"><table class="heatmap"><tr><th class="name-col">Ученик</th>""")
            chunk.forEach { key ->
                sb.append("<th>${escape(subjectLabels[key] ?: key)}</th>")
            }
            sb.append("</tr>")
            students.forEach { (sid, name) ->
                sb.append("<tr><td class=\"name-col\">${escape(name)}</td>")
                chunk.forEach { sub ->
                    val score = scores[sid to sub] ?: 0
                    sb.append(heatmapCell(score, maxAbs))
                }
                sb.append("</tr>")
            }
            sb.append("</table></div>")
        }
        return sb.toString()
    }

    fun rowColumnHeatmap(
        rowLabels: List<String>,
        colLabels: List<String>,
        cellScore: (row: String, col: String) -> Int,
        rowHeader: String,
    ): String {
        if (rowLabels.isEmpty() || colLabels.isEmpty()) return ""
        val scores = rowLabels.flatMap { row ->
            colLabels.map { col -> cellScore(row, col) }
        }
        val maxAbs = max(scores.maxOfOrNull { abs(it) } ?: 1, 1)
        val sb = StringBuilder("""<table class="heatmap"><tr><th class="name-col">${escape(rowHeader)}</th>""")
        colLabels.forEach { col -> sb.append("<th>${escape(col)}</th>") }
        sb.append("</tr>")
        rowLabels.forEach { row ->
            sb.append("<tr><td class=\"name-col\">${escape(row)}</td>")
            colLabels.forEach { col ->
                sb.append(heatmapCell(cellScore(row, col), maxAbs))
            }
            sb.append("</tr>")
        }
        sb.append("</table>")
        return sb.toString()
    }

    private fun heatmapBackground(score: Int, maxAbs: Int): String {
        if (score == 0) return "#F0F3F2"
        val t = (abs(score).toFloat() / maxOf(maxAbs, 1)).coerceIn(0.12f, 1f)
        return if (score > 0) lerpColor("#E3F5EA", "#3D9A62", t) else lerpColor("#FCEAEA", "#C95555", t)
    }

    private fun heatmapForeground(score: Int, maxAbs: Int): String {
        if (score == 0) return "#8A9E97"
        val t = abs(score).toFloat() / maxOf(maxAbs, 1)
        return if (t > 0.45f) "#FFFFFF" else "#4A5F5A"
    }

    private fun lerpColor(from: String, to: String, t: Float): String {
        fun comp(hex: String, shift: Int) = hex.removePrefix("#").substring(shift, shift + 2).toInt(16)
        val rf = comp(from, 0); val gf = comp(from, 2); val bf = comp(from, 4)
        val rt = comp(to, 0); val gt = comp(to, 2); val bt = comp(to, 4)
        val r = (rf + (rt - rf) * t).roundToInt().coerceIn(0, 255)
        val g = (gf + (gt - gf) * t).roundToInt().coerceIn(0, 255)
        val b = (bf + (bt - bf) * t).roundToInt().coerceIn(0, 255)
        return "#%02X%02X%02X".format(r, g, b)
    }
}
