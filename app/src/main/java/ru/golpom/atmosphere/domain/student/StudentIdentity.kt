/**
 * Идентификация ученика: ФИО и ключ.
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain.student

import java.time.Year

/**
 * Единый ключ ученика для слияния файлов с разных устройств: класс + ФИО.
 * Один и тот же ученик в 5-А у географии и математики получает один [canonicalStudentId].
 */
object StudentIdentity {

    fun normalizeClassId(raw: String): String {
        val t = raw.trim().replace(" ", "")
        val regex = Regex("^(\\d+)[-]?([а-яА-ЯёЁa-zA-Z]+)$")
        val m = regex.find(t)
        return if (m != null) {
            "${m.groupValues[1]}-${m.groupValues[2].uppercase()}"
        } else {
            t.uppercase()
        }
    }

    fun normalizeName(raw: String): String {
        val parts = raw.trim().split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isEmpty()) return ""
        val name = parts[0]
        return name.substring(0, 1).uppercase() + name.substring(1).lowercase()
    }

    fun canonicalStudentId(
        classId: String,
        firstName: String,
        lastName: String,
        year: Int = Year.now().value,
    ): String {
        val c = normalizeClassId(classId)
        val fn = normalizeName(firstName)
        val ln = normalizeName(lastName)
        val fnSeg = fn.replace(unsafeChars, "_").ifEmpty { "x" }
        val lnSeg = ln.replace(unsafeChars, "_").ifEmpty { "x" }
        return "$year-$c-$lnSeg-$fnSeg"
    }

    fun namesUsable(firstName: String?, lastName: String?): Boolean =
        !firstName.isNullOrBlank() && !lastName.isNullOrBlank()

    private val unsafeChars = Regex("[\\\\/:*?\"<>|\\r\\n]")
}
