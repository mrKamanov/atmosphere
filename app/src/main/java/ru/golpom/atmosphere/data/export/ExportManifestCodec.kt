/**
 * Сериализация manifest.json в пакете экспорта.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ExportManifestCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(dto: ExportManifestDto): String = json.encodeToString(dto)

    fun decode(text: String): ExportManifestDto = json.decodeFromString<ExportManifestDto>(text.trim())
}
