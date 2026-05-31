/**
 * Room-сущность Notification.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val body: String,
    val timestampMillis: Long,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    val relatedId: String? = null,
    val metadataJson: String? = null,
)
