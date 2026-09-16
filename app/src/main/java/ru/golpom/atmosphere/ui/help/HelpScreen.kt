/**
 * Экран справки: инструкции по использованию приложения.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.NavigationBarScrollSpacer
import ru.golpom.atmosphere.ui.theme.PrimaryBlue
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val InfoBg = Color(0xFFE3F2FD)
private val InfoFg = Color(0xFF1E5FBF)
private val WarnBg = Color(0xFFFCEFE3)
private val WarnFg = Color(0xFF9A5B12)
private val WarnStripe = Color(0xFFF59E0B)
private val TipBg = Color(0xFFE3F5EA)
private val TipFg = Color(0xFF2E7D32)
private val GreenText = Color(0xFF166534)
private val RedText = Color(0xFF991B1B)
private val BorderSoft = Color(0xFFE6ECE9)

private enum class Tone { DEFAULT, ACCENT, POSITIVE, NEGATIVE }

private data class Seg(
    val text: String,
    val bold: Boolean = false,
    val tone: Tone = Tone.DEFAULT,
)

private sealed interface Block {
    data class Text(val segs: List<Seg>) : Block
    data class Step(val number: Int, val segs: List<Seg>) : Block
    data class Bullet(val segs: List<Seg>) : Block
    data class Callout(val segs: List<Seg>, val kind: CalloutKind) : Block
}

private enum class CalloutKind { INFO, TIP, WARNING }

private data class HelpCard(
    val title: String,
    val blocks: List<Block>,
)

private data class HelpSection(
    val title: String,
    val cards: List<HelpCard>,
)

private fun t(text: String): Seg = Seg(text)
private fun b(text: String): Seg = Seg(text, bold = true)
private fun a(text: String): Seg = Seg(text, bold = true, tone = Tone.ACCENT)
private fun p(text: String): Seg = Seg(text, bold = true, tone = Tone.POSITIVE)
private fun n(text: String): Seg = Seg(text, bold = true, tone = Tone.NEGATIVE)

private val helpSections = listOf(
    HelpSection(
        title = "Общее описание",
        cards = listOf(
            HelpCard(
                title = "Что такое «Атмосфера»",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Мобильный журнал дисциплины: "),
                            b("учитель"),
                            t(" отмечает поведение на уроке за пару секунд, "),
                            b("завуч"),
                            t(" собирает сводку по классам и ученикам. "),
                            b("Работает без интернета"),
                            t(" — данные хранятся только на вашем устройстве."),
                        ),
                    ),
                ),
            ),
            HelpCard(
                title = "Два режима",
                blocks = listOf(
                    Block.Bullet(listOf(b("Учитель: "), t("классы, ученики, расписание, отметки, отчёты."))),
                    Block.Bullet(listOf(b("Завуч: "), t("импорт отчётов, аналитика по школе, классу и ученику, экспорт в HTML и PDF."))),
                    Block.Callout(
                        listOf(t("Переключаются режимы иконкой "), a("«человек»"), t(" в правом верхнем углу.")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Как выгрузить файлы из «Моя школа»",
        cards = listOf(
            HelpCard(
                title = "Список учеников (журнал класса)",
                blocks = listOf(
                    Block.Step(1, listOf(t("В «Моя школа» откройте "), b("журнал класса"), t(" по вашему предмету — например, "), a("География, 9-Н"))),
                    Block.Step(2, listOf(t("Нажмите "), b("«Экспорт журнала в Excel»"))),
                    Block.Step(3, listOf(t("В открывшемся меню выберите вариант "), a("«Расширенный»"))),
                    Block.Step(4, listOf(t("Скачайте файл и импортируйте его в «Атмосфера»: "), a("«Мои ученики» → ⋮ → «Загрузить из Моя школа»"))),
                    Block.Callout(
                        listOf(t("Важно: выбирайте именно "), b("«Расширенный»"), t(", а не «Базовый» — только в расширенном файле есть полный список учеников класса.")),
                        CalloutKind.WARNING,
                    ),
                ),
            ),
            HelpCard(
                title = "Расписание",
                blocks = listOf(
                    Block.Step(1, listOf(t("В «Моя школа» откройте "), b("«Моё расписание»"))),
                    Block.Step(2, listOf(t("Нажмите "), b("«Экспорт»"), t(" и сохраните файл в Excel"))),
                    Block.Step(3, listOf(t("В «Атмосфера»: "), a("«Расписание» → ⋮ → «Загрузить из Моя школа»"))),
                    Block.Callout(
                        listOf(b("Один нюанс: "), t("после загрузки расписания откройте каждый урок и карандашом проверьте, что класс привязан верно — иначе на уроке не будет списка детей.")),
                        CalloutKind.INFO,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Работа в приложении",
        cards = listOf(
            HelpCard(
                title = "Что сделать в первый раз",
                blocks = listOf(
                    Block.Step(1, listOf(t("Откройте "), a("Настройки"), t(" (шестерёнка) и укажите "), b("имя"), t(" и "), b("фамилию"), t(" для отчёта."))),
                    Block.Step(2, listOf(t("Добавьте класс: "), a("«Мои классы» → «+»"), t(" и введите обозначение, например "), b("«9-Н»"))),
                    Block.Step(3, listOf(t("Добавьте учеников — вручную или из «Моя школа» (см. выше)."))),
                    Block.Step(4, listOf(t("Загрузите расписание (см. выше) — уроки появятся на главном экране."))),
                ),
            ),
            HelpCard(
                title = "Ученики: как добавить и что с ними делать",
                blocks = listOf(
                    Block.Bullet(listOf(b("Вручную: "), t("«Мои ученики» → «+» → фамилия, имя, класс."))),
                    Block.Bullet(listOf(b("Из файла: "), t("«Мои ученики» → ⋮ → «Загрузить из Моя школа». В имени файла должен быть класс — например, "), a("9-Н.xlsx"))),
                    Block.Bullet(listOf(b("Из шаблона: "), t("⋮ → «Скачать шаблон» → заполнить Excel → ⋮ → «Загрузить из шаблона»."))),
                    Block.Bullet(listOf(b("Перевести: "), t("кнопка на карточке ученика — сменить класс."))),
                    Block.Bullet(listOf(b("Удалить: "), t("корзина на карточке. Можно "), p("архивировать "), t("(история останется) или "), n("удалить навсегда"), t("."))),
                ),
            ),
            HelpCard(
                title = "Расписание: что можно делать",
                blocks = listOf(
                    Block.Bullet(listOf(b("Добавить урок: "), t("«+» → время, длительность (30/35/40/45 мин), предмет, класс."))),
                    Block.Bullet(listOf(t("При пересечении времени приложение "), b("предупредит"), t(" о конфликте."))),
                    Block.Text(listOf(b("Редактировать урок: "), t("карандаш на карточке. Удалить — корзина."))),
                ),
            ),
            HelpCard(
                title = "Отметки на уроке",
                blocks = listOf(
                    Block.Bullet(
                        listOf(
                            b("Короткий тап"),
                            t(" по ученику — шторка с отметками. "),
                            b("Долгий тап"),
                            t(" — профиль с историей."),
                        ),
                    ),
                    Block.Bullet(listOf(p("Положительные: "), t("Старается, Помощь классу, Прогресс, Примерное поведение — все "), p("+1"), t("."))),
                    Block.Bullet(listOf(n("Отрицательные: "), t("Срыв дисциплины, Гаджет, Опоздание, Не готов, Драка, Ненормативная лексика — все "), n("−1"), t("."))),
                ),
            ),
            HelpCard(
                title = "Родительские собрания",
                blocks = listOf(
                    Block.Bullet(listOf(t("Календарь на главном экране. Зелёные метки — даты с собраниями."))),
                    Block.Bullet(listOf(b("Добавить: "), t("выбрать дату → «+» → тема, класс, время, заметки."))),
                    Block.Bullet(listOf(b("Изменить или удалить: "), t("нажать на карточку собрания → «Редактировать» / «Удалить»."))),
                ),
            ),
            HelpCard(
                title = "Напоминания",
                blocks = listOf(
                    Block.Bullet(listOf(a("О собраниях: "), t("за день и за 3 часа до собрания."))),
                    Block.Bullet(listOf(a("Об уроках: "), t("после завершения урока без отметок."))),
                    Block.Bullet(listOf(a("Системные: "), t("показывать уведомления, даже когда приложение закрыто."))),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Отчёт завучу",
        cards = listOf(
            HelpCard(
                title = "Как работает связка",
                blocks = listOf(
                    Block.Text(listOf(b("Учитель"), t(" формирует файл отчёта и отправляет его через мессенджер или почту; "), b("завуч"), t(" импортирует этот файл у себя и смотрит сводку."))),
                ),
            ),
            HelpCard(
                title = "Учитель: выгрузка отчёта",
                blocks = listOf(
                    Block.Step(1, listOf(t("«Настройки» → «Отправить отчёт завучу»."))),
                    Block.Step(2, listOf(t("Выберите объём: "), a("все классы"), t(", "), a("все отметки"), t(" или "), a("один предмет"), t("."))),
                    Block.Step(3, listOf(t("Выберите период: "), a("день / неделя / месяц / учебный год"), t("."))),
                    Block.Step(4, listOf(t("Нажмите "), b("«Создать отчёт»"), t("."))),
                    Block.Step(5, listOf(t("Нажмите "), b("«Отправить»"), t(" и перешлите файл завучу. Или «Сохранить» — файл останется на телефоне."))),
                    Block.Callout(
                        listOf(t("Файл отчёта (формат "), b(".atmo"), t(") зашифрован и "), b("открывается только в «Атмосфера»"), t(" на устройстве завуча.")),
                        CalloutKind.INFO,
                    ),
                ),
            ),
            HelpCard(
                title = "Завуч: импорт и аналитика",
                blocks = listOf(
                    Block.Step(1, listOf(t("Переключитесь в режим завуча (иконка "), a("«человек»"), t(")."))),
                    Block.Step(2, listOf(t("Нажмите "), a("«↑» (Загрузить)"), t(" и выберите файлы .atmo от учителей."))),
                    Block.Step(3, listOf(t("Управляйте отчётами в блоке "), b("«Источники данных»"), t(" — включать/выключать/удалять."))),
                    Block.Callout(
                        listOf(t("Выключенные отчёты "), b("не удаляются"), t(" — данные останутся, просто их не видно в сводке.")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
            HelpCard(
                title = "Что видит завуч на дашборде",
                blocks = listOf(
                    Block.Bullet(listOf(b("Сводка "), t("по школе за период (неделя / месяц / произвольный)."))),
                    Block.Bullet(listOf(b("Динамика "), t("— график изменения баллов по неделям."))),
                    Block.Bullet(listOf(b("Рейтинг классов "), t("— таблица, нажмите на класс для деталей."))),
                    Block.Bullet(listOf(b("Тепловая карта "), t("— классы по дням недели."))),
                    Block.Bullet(listOf(b("Ритм недели "), t("— в какие дни лучше/хуже."))),
                    Block.Bullet(listOf(b("Ученики "), t("— «Кого похвалить» и «Кому уделить внимание»."))),
                    Block.Bullet(listOf(b("Тезисы для педсовета "), t("— готовые формулировки."))),
                    Block.Bullet(listOf(b("Поиск ученика "), t("— по фамилии или имени."))),
                ),
            ),
            HelpCard(
                title = "Завуч: экспорт отчётов",
                blocks = listOf(
                    Block.Bullet(listOf(b("Школа: "), t("иконка «↓» на дашборде."))),
                    Block.Bullet(listOf(b("Класс: "), t("откройте класс → «↓»."))),
                    Block.Bullet(listOf(b("Ученик: "), t("откройте профиль → «↓»."))),
                    Block.Callout(
                        listOf(t("Форматы экспорта: "), a("HTML"), t(" (для просмотра в браузере) и "), a("PDF"), t(" (для печати и архива).")),
                        CalloutKind.INFO,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Данные и советы",
        cards = listOf(
            HelpCard(
                title = "Управление данными",
                blocks = listOf(
                    Block.Bullet(listOf(b("Очистить все отметки: "), t("удалит баллы, но классы, учеников и расписание сохранит."))),
                    Block.Bullet(listOf(b("Очистить архив: "), t("удалит архивных учеников и старые уведомления."))),
                    Block.Bullet(listOf(n("Удалить все данные: "), t("полный сброс — действие необратимо."))),
                    Block.Callout(
                        listOf(t("Все данные хранятся "), b("только на вашем устройстве"), t(". Разработчику они не передаются.")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
            HelpCard(
                title = "Полезные советы",
                blocks = listOf(
                    Block.Bullet(listOf(b("Быстрый старт: "), t("класс → ученики → расписание → проверьте привязку классов → отмечайте."))),
                    Block.Bullet(listOf(b("Имя файла: "), t("при импорте из «Моя школа» имя файла должно содержать класс, например "), a("9-Н.xlsx"), t("."))),
                    Block.Bullet(listOf(b("Резервная копия: "), t("регулярно отправляйте отчёты завучу — это и есть резервная копия."))),
                    Block.Bullet(listOf(b("Архивация: "), t("при переводе ученика архивируйте, а не удаляйте — история останется."))),
                ),
            ),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = { Text("Справка", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            helpSections.forEach { section ->
                item {
                    SectionHeader(section.title)
                }
                items(section.cards) { card ->
                    HelpCardView(card)
                }
            }
            item {
                NavigationBarScrollSpacer()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, top = 6.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LessonGreen),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun HelpCardView(card: HelpCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                card.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            card.blocks.forEach { block ->
                when (block) {
                    is Block.Text -> RichText(block.segs)
                    is Block.Step -> StepRow(block)
                    is Block.Bullet -> BulletRow(block)
                    is Block.Callout -> CalloutBox(block)
                }
            }
        }
    }
}

@Composable
private fun RichText(segs: List<Seg>) {
    Text(
        segs.toAnnotated(TextSecondary),
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}

private fun List<Seg>.toAnnotated(baseColor: Color): AnnotatedString = buildAnnotatedString {
    forEach { seg ->
        val color = when (seg.tone) {
            Tone.ACCENT -> PrimaryBlue
            Tone.POSITIVE -> GreenText
            Tone.NEGATIVE -> RedText
            Tone.DEFAULT -> baseColor
        }
        pushStyle(
            SpanStyle(
                color = color,
                fontWeight = if (seg.bold) FontWeight.Bold else FontWeight.Normal,
            ),
        )
        append(seg.text)
        pop()
    }
}

@Composable
private fun StepRow(step: Block.Step) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LessonGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${step.number}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LessonGreen,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            step.segs.toAnnotated(TextSecondary),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BulletRow(bullet: Block.Bullet) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            bullet.segs.toAnnotated(TextSecondary),
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun CalloutBox(callout: Block.Callout) {
    val (bg, fg, icon) = when (callout.kind) {
        CalloutKind.INFO -> Triple(InfoBg, InfoFg, Icons.Default.Info)
        CalloutKind.TIP -> Triple(TipBg, TipFg, Icons.Default.TipsAndUpdates)
        CalloutKind.WARNING -> Triple(WarnBg, WarnFg, Icons.Default.Warning)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            callout.segs.toAnnotated(fg),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = fg,
        )
    }
}