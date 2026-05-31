/**
 * Состояние списка учеников.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.students

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.excel.MySchoolStudentParser
import ru.golpom.atmosphere.data.excel.StudentExcelParser
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.student.StudentIdentity

enum class StudentFilter { ACTIVE, ARCHIVED, ALL }

@HiltViewModel
class StudentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(StudentFilter.ACTIVE)
    val filter: StateFlow<StudentFilter> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val students: StateFlow<List<StudentEntity>> = _filter.flatMapLatest { f ->
        when (f) {
            StudentFilter.ACTIVE -> catalogRepository.observeTeacherStudents()
            StudentFilter.ARCHIVED -> catalogRepository.observeTeacherArchivedStudents()
            StudentFilter.ALL -> catalogRepository.observeTeacherStudentsAll()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: StudentFilter) {
        _filter.value = f
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importStatusMessage = MutableStateFlow("")
    val importStatusMessage: StateFlow<String> = _importStatusMessage.asStateFlow()

    fun addStudent(firstName: String, lastName: String, classId: String) {
        val f = StudentIdentity.normalizeName(firstName)
        val l = StudentIdentity.normalizeName(lastName)
        val cls = StudentIdentity.normalizeClassId(classId)
        if (f.isBlank() || l.isBlank() || cls.isBlank()) {
            _userMessage.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            val id = "${java.time.Year.now().value}-$cls-${System.currentTimeMillis() % 1000}"
            catalogRepository.addStudent(id, f, l, cls)
            _userMessage.value = "Ученик $l $f добавлен в $cls"
        }
    }

    fun moveStudent(studentId: String, newClassId: String) {
        viewModelScope.launch {
            catalogRepository.moveStudent(studentId, newClassId)
            _userMessage.value = "Ученик переведён в $newClassId"
        }
    }

    fun archiveStudent(studentId: String) {
        viewModelScope.launch {
            catalogRepository.archiveStudent(studentId)
            _userMessage.value = "Ученик архивирован"
        }
    }

    fun hardDeleteStudent(studentId: String) {
        viewModelScope.launch {
            catalogRepository.hardDeleteStudent(studentId)
            _userMessage.value = "Ученик удалён навсегда"
        }
    }

    fun restoreStudent(studentId: String) {
        viewModelScope.launch {
            catalogRepository.restoreStudent(studentId)
            _userMessage.value = "Ученик восстановлен"
        }
    }

    fun importTemplate(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Загружаем список учеников…"
            val result = try {
                withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext "Ошибка открытия файла"
                val parsed = StudentExcelParser.parse(input)
                if (parsed.students.isEmpty()) {
                    if (parsed.errors.isNotEmpty()) parsed.errors.joinToString("\n")
                    else "Файл не содержит учеников"
                } else {
                    val entities = StudentExcelParser.toEntities(parsed.students)
                    val count = catalogRepository.importStudents(entities)
                    val errMsg = if (parsed.errors.isNotEmpty()) "\n\nЗамечания:\n${parsed.errors.joinToString("\n")}" else ""
                    "Загружено $count учеников$errMsg"
                }
            }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            _userMessage.value = result
        }
    }

    fun importMySchool(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Загружаем учеников из «Моя школа»…"
            val result = try {
                withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext "Ошибка открытия файла"
                val fileName = withContext(Dispatchers.IO) {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { if (it.moveToFirst()) it.getString(it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)) else null }
                } ?: uri.lastPathSegment ?: ""
                val classId = MySchoolStudentParser.extractClassId(fileName)
                if (classId.isBlank()) return@withContext "Не удалось определить класс из имени файла"
                val parsed = MySchoolStudentParser.parse(input)
                if (parsed.isEmpty()) return@withContext "Файл не содержит учеников"
                val entities = StudentExcelParser.toEntities(
                    parsed.map { StudentExcelParser.ParsedStudent(it.firstName, it.lastName, classId) },
                )
                val count = catalogRepository.importStudents(entities)
                "Загружено $count учеников в $classId"
            }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            _userMessage.value = result
        }
    }

    fun generateTemplate(callback: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Готовим шаблон списка учеников…"
            val bytes = try {
                withContext(Dispatchers.IO) {
                    val baos = ByteArrayOutputStream()
                    try {
                        StudentExcelParser.generateTemplate(baos)
                        baos.toByteArray()
                    } catch (t: Throwable) {
                        _userMessage.value = "Ошибка создания шаблона: ${t.message}"
                        null
                    }
                }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            callback(bytes)
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }
}
