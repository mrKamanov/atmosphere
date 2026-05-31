/**
 * Карточка одного класса на домашнем экране учителя: кнопки запуска урока по предметам.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.domain.SubjectKeys

@Composable
fun TeacherClassCard(
    classEntity: ClassEntity,
    onOpenLesson: (classId: String, subjectKey: String) -> Unit,
) {
    Card {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(classEntity.classId, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { onOpenLesson(classEntity.classId, SubjectKeys.GEOGRAPHY) }) {
                Text("Урок: география")
            }
            Button(onClick = { onOpenLesson(classEntity.classId, SubjectKeys.MATHEMATICS) }) {
                Text("Урок: математика")
            }
        }
    }
}
