/**
 * Устройство учителя в локальной сети.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.transfer

data class LanDevice(
    val serviceId: String,
    val displayName: String,
    val shortId: String,
    val host: String,
    val port: Int,
    val isDeputy: Boolean,
) {
    val label: String
        get() = if (shortId.isBlank()) displayName else "$displayName · $shortId"
}
