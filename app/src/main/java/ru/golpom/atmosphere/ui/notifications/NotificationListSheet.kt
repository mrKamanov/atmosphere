/**
 * Шторка списка уведомлений.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.data.local.entity.NotificationEntity
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationListSheet(
    notifications: List<NotificationEntity>,
    onDismiss: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Уведомления", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Готово", fontWeight = FontWeight.Medium, color = TextPrimary.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ChipBg,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Нет уведомлений", fontSize = 16.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("Всё спокойно", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.id }) { n ->
                    NotificationCard(
                        notification = n,
                        onDismiss = { onDismiss(n.id) },
                        onDelete = { onDelete(n.id) },
                        onMarkRead = { onMarkRead(n.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) CardBg else CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 3.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val icon = when (notification.type) {
                "meeting_reminder" -> Icons.Default.CalendarMonth
                else -> Icons.Default.Notifications
            }
            val iconBg = when (notification.type) {
                "meeting_reminder" -> Color(0xFFE3F2FD)
                "lesson_grade_prompt" -> Color(0xFFFFF3E0)
                else -> ChipBg
            }
            val iconTint = when (notification.type) {
                "meeting_reminder" -> Color(0xFF1565C0)
                "lesson_grade_prompt" -> Color(0xFFE65100)
                else -> TextSecondary
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    notification.body,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatTimestamp(notification.timestampMillis),
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp), tint = TextSecondary.copy(alpha = 0.5f))
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val zdt = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    return if (zdt.toLocalDate() == now.toLocalDate()) {
        "сегодня в ${zdt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    } else if (zdt.toLocalDate() == now.toLocalDate().minusDays(1)) {
        "вчера в ${zdt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    } else {
        zdt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
    }
}
