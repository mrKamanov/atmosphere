/**
 * Кодирование и декодирование `Class_Config.json` (UTF-8, pretty-print для ручного просмотра).
 * Data-слой.
 */
package ru.golpom.atmosphere.data.config

import kotlinx.serialization.json.Json

object ClassConfigJsonCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(dto: ClassConfigFileDto): String = json.encodeToString(ClassConfigFileDto.serializer(), dto)

    fun decode(text: String): ClassConfigFileDto = json.decodeFromString(ClassConfigFileDto.serializer(), text)
}
