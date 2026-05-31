/**
 * Кнопка экспорта HTML-отчёта на экранах завуча.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand

@Composable
fun DeputyExportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Экспорт",
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AtmosphereBrand.TealAccent,
            )
            Text(
                label,
                modifier = Modifier.padding(start = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AtmosphereBrand.TealAccent,
            )
        }
    }
}
