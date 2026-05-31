/**
 * Конвертация HTML-отчёта в PDF через WebView и системный Print API.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import android.content.Context
import android.print.DeputyHtmlPdfWriter
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object HtmlReportPdfExporter {
    const val MIME_TYPE = "application/pdf"

    suspend fun render(context: Context, html: String): ByteArray = withContext(Dispatchers.Main) {
        withTimeout(45_000) {
            suspendCancellableCoroutine { cont ->
                val webView = WebView(context.applicationContext)
                webView.settings.apply {
                    defaultTextEncodingName = "utf-8"
                    blockNetworkLoads = true
                    javaScriptEnabled = false
                }
                var finished = false
                fun cleanup() {
                    if (!finished) {
                        finished = true
                        webView.destroy()
                    }
                }
                cont.invokeOnCancellation { cleanup() }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (cont.isCancelled || finished) return
                        DeputyHtmlPdfWriter.write(context.applicationContext, webView, object : DeputyHtmlPdfWriter.ResultCallback {
                            override fun onSuccess(pdfBytes: ByteArray) {
                                cleanup()
                                cont.resume(pdfBytes)
                            }

                            override fun onFailure() {
                                cleanup()
                                cont.resumeWithException(IllegalStateException("Не удалось сформировать PDF"))
                            }
                        })
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?,
                    ) {
                        if (cont.isCancelled || finished) return
                        cleanup()
                        cont.resumeWithException(IllegalStateException(description ?: "Ошибка загрузки отчёта"))
                    }
                }

                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }

    fun pdfFileName(htmlFileName: String): String {
        val base = htmlFileName.removeSuffix(".html").removeSuffix(".HTML")
        return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
    }
}
