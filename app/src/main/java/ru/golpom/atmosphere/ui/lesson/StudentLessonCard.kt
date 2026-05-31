/**
 * Карточка ученика на экране урока: короткий тап — отметка, долгий — профиль (§5.1 ТЗ).
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.lesson

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.golpom.atmosphere.data.local.model.LessonStudentRow
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.AtmosphereScoreBadge

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentLessonCard(
    row: LessonStudentRow,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val (bg, border) = when {
        row.balance > 0 -> AtmosphereBrand.PositiveSoft to AtmosphereBrand.PositiveDeep.copy(alpha = 0.45f)
        row.balance < 0 -> AtmosphereBrand.NegativeSoft to AtmosphereBrand.NegativeDeep.copy(alpha = 0.45f)
        else -> AtmosphereBrand.Paper to AtmosphereBrand.Rule.copy(alpha = 0.35f)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(border),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 7.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 5.dp,
            draggedElevation = 8.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        row.lastName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        row.firstName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtmosphereBrand.InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AtmosphereScoreBadge(row.balance)
            }
        }
    }
}
