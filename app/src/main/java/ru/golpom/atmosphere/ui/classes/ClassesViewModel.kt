/**
 * Состояние списка классов.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.student.StudentIdentity

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun createClass(raw: String) {
        val id = StudentIdentity.normalizeClassId(raw)
        if (id.isBlank()) {
            _userMessage.value = "Введите код класса"
            return
        }
        viewModelScope.launch {
            catalogRepository.createClass(id)
            _userMessage.value = "Класс $id добавлен"
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            catalogRepository.deleteClass(classId)
            _userMessage.value = "Класс $classId удалён"
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }
}
