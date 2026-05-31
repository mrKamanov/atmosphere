/**
 * DTO manifest.json пакета экспорта.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportManifestDto(
    val format_version: Int = 1,
    val teacher_profile_id: String,
    val teacher_profile_short_id: String,
    val teacher_last_name: String,
    val teacher_display_name: String = "",
    val exported_at_millis: Long,
    val period_kind: String,
    val period_label: String,
    val period_from_millis: Long? = null,
    val period_to_millis: Long? = null,
    val scope_tag: String,
    val record_count: Int,
    val sealed: Boolean = false,
    val format: String = "atmo_v1",
    val identity_merge: Boolean = false,
)
