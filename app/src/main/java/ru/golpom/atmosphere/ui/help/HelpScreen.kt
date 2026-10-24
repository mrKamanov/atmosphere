/**
 * Экран справки: инструкции по использованию приложения.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    /** Открыта ли карточка сразу; остальные раскрываются нажатием на заголовок. */
    val expandedByDefault: Boolean = false,
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
        title = "С чего начать",
        cards = listOf(
            HelpCard(
                title = "Что это за приложение",
                expandedByDefault = true,
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("«Атмосфера» — журнал поведения у вас в телефоне. На уроке вы одним касанием отмечаете, "),
                            t("кто старался, а кто мешал. Раз в неделю или в месяц отправляете завучу один файл, "),
                            t("и у него складывается картина по всей школе."),
                        ),
                    ),
                    Block.Text(
                        listOf(
                            t("Интернет не нужен. Все данные хранятся "),
                            b("только в вашем телефоне"),
                            t(" — разработчику ничего не отправляется."),
                        ),
                    ),
                ),
            ),
            HelpCard(
                title = "Первая настройка",
                expandedByDefault = true,
                blocks = listOf(
                    Block.Text(listOf(t("Займёт минут десять. Делается один раз."))),
                    Block.Step(
                        1,
                        listOf(
                            t("Откройте "),
                            a("Настройки"),
                            t(" (шестерёнка вверху справа). Впишите, как к вам обращаться, и фамилию — "),
                            t("её завуч увидит в ваших отчётах."),
                        ),
                    ),
                    Block.Step(2, listOf(t("Загрузите учеников. Проще всего — из «Моя школа», как это сделать, написано ниже. Можно и вручную."))),
                    Block.Step(3, listOf(t("Загрузите расписание — тоже из «Моя школа» или вручную."))),
                    Block.Step(
                        4,
                        listOf(
                            t("Откройте "),
                            a("Расписание"),
                            t(" и проверьте, что у уроков стоят "),
                            b("те же классы"),
                            t(", что и у учеников."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Это самая частая причина пустого урока: в расписании класс записан как «9Л», "),
                            t("а ученики сидят в «9-Л». Приложение считает это разными классами."),
                        ),
                        CalloutKind.WARNING,
                    ),
                ),
            ),
            HelpCard(
                title = "Учитель и завуч в одном приложении",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("У приложения два режима. В режиме "),
                            b("учителя"),
                            t(" вы ведёте свои классы и ставите отметки. В режиме "),
                            b("завуча"),
                            t(" собираете отчёты коллег и смотрите сводку по школе."),
                        ),
                    ),
                    Block.Callout(
                        listOf(t("Переключаются режимы иконкой "), a("человечка"), t(" вверху главного экрана. Данные при этом не теряются.")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Загрузка из «Моя школа»",
        cards = listOf(
            HelpCard(
                title = "Список учеников",
                blocks = listOf(
                    Block.Step(1, listOf(t("В «Моя школа» откройте "), b("журнал класса"), t(" по своему предмету, например «География, 9-Н»."))),
                    Block.Step(2, listOf(t("Нажмите "), b("«Экспорт журнала в Excel»"), t(" и выберите вариант "), b("«Расширенный»"), t(" — только в нём есть полный список детей."))),
                    Block.Step(3, listOf(t("В «Атмосфере» откройте "), a("Мои ученики → ⋮ → Загрузить из Моя школа"), t(" и выберите скачанный файл."))),
                    Block.Callout(
                        listOf(
                            t("Класс приложение узнаёт по "),
                            b("названию файла"),
                            t(". В нём должны быть цифра и буква класса: «9-Н.xlsx» или «География 9-Н.xlsx». "),
                            t("Если класс не распознается, переименуйте файл и загрузите снова."),
                        ),
                        CalloutKind.INFO,
                    ),
                    Block.Text(listOf(t("Повторная загрузка того же файла не создаст дублей: дети, которые уже есть в списке, пропускаются."))),
                ),
            ),
            HelpCard(
                title = "Расписание",
                blocks = listOf(
                    Block.Step(1, listOf(t("В «Моя школа» откройте "), b("«Моё расписание»"), t(", нажмите "), b("«Экспорт»"), t(" и сохраните файл Excel."))),
                    Block.Step(2, listOf(t("В «Атмосфере»: "), a("Расписание → ⋮ → Загрузить из «Моя школа»"), t("."))),
                    Block.Step(3, listOf(t("Пройдитесь по урокам и проверьте классы (см. «Первая настройка», шаг 4)."))),
                ),
            ),
            HelpCard(
                title = "Если «Моя школа» нет — через шаблон",
                blocks = listOf(
                    Block.Step(1, listOf(t("В «Мои ученики» или «Расписание» нажмите "), a("⋮ → Скачать шаблон"), t("."))),
                    Block.Step(2, listOf(t("Заполните его в Excel или Google Таблицах. Для учеников нужны три столбца: фамилия, имя, класс."))),
                    Block.Step(3, listOf(t("Сохраните и загрузите обратно: "), a("⋮ → Загрузить из шаблона"), t("."))),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "На уроке",
        cards = listOf(
            HelpCard(
                title = "Как поставить отметку",
                blocks = listOf(
                    Block.Step(
                        1,
                        listOf(
                            t("Во время урока нажмите большую кнопку "),
                            b("«Урок»"),
                            t(" на главном экране. Другой урок можно открыть из "),
                            a("Расписания"),
                            t(" — просто нажмите на него."),
                        ),
                    ),
                    Block.Step(2, listOf(t("Коснитесь ученика — внизу появятся варианты отметок."))),
                    Block.Step(3, listOf(t("Выберите нужную. Отметка сохраняется сразу, ничего подтверждать не надо."))),
                    Block.Text(
                        listOf(
                            t("На карточке ученика виден его баланс за сегодня. Если дети не помещаются на экране, "),
                            t("сведите или разведите два пальца — сетка уменьшится или увеличится."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Ошиблись? "),
                            b("Подержите палец"),
                            t(" на ученике — откроется его профиль. Там найдите лишнюю запись и удалите её корзиной."),
                        ),
                        CalloutKind.TIP,
                    ),
                ),
            ),
            HelpCard(
                title = "Какие бывают отметки",
                blocks = listOf(
                    Block.Text(listOf(t("Каждая отметка — это "), p("+1"), t(" или "), n("−1"), t(" к баллу ученика."))),
                    Block.Bullet(listOf(p("Хорошее: "), t("старается, помощь классу, прогресс, примерное поведение."))),
                    Block.Bullet(
                        listOf(
                            n("Замечания: "),
                            t("срыв дисциплины, гаджет на уроке, опоздание, без подготовки, драка, ненормативная лексика, "),
                            t("разговаривает, перебивает, списывает, забыл(а), без формы, портит имущество."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Мелкие замечания — разговоры, перебивания, списывание, форма, имущество — "),
                            t("в сводке завуча объединяются в одну строку «Замечания по поведению», чтобы не загромождать отчёт."),
                        ),
                        CalloutKind.INFO,
                    ),
                ),
            ),
            HelpCard(
                title = "Замечание на перемене",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Если ученик нарушил дисциплину не на уроке, а в коридоре или столовой, откройте "),
                            a("Мои ученики"),
                            t(" и нажмите красный "),
                            b("треугольник"),
                            t(" рядом с ним. Приложение переспросит, а затем запишет «Срыв дисциплины» (−1) с пометкой «Вне урока»."),
                        ),
                    ),
                ),
            ),
            HelpCard(
                title = "Что показывает главный экран",
                blocks = listOf(
                    Block.Bullet(listOf(b("«Сейчас: …»"), t(" — какой урок идёт: предмет, класс, время. Кнопка «Урок» в это время активна."))),
                    Block.Bullet(listOf(b("«Перемена»"), t(" — урока сейчас нет, но сегодня ещё будут."))),
                    Block.Bullet(listOf(b("«Следующий: …»"), t(" — ближайший урок, сегодня или в следующий учебный день."))),
                    Block.Text(listOf(t("Ниже — ваши классы, ученики и ближайшее родительское собрание. Всё обновляется само, экран перезагружать не нужно."))),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Ученики и классы",
        cards = listOf(
            HelpCard(
                title = "Список учеников",
                blocks = listOf(
                    Block.Text(listOf(t("В "), a("Мои ученики"), t(" есть поиск по фамилии, имени или классу и переключатели «Активные», «Архив», «Все»."))),
                    Block.Bullet(listOf(b("Добавить ребёнка: "), t("кнопка «+», затем фамилия, имя и класс."))),
                    Block.Bullet(listOf(b("Перевести в другой класс: "), t("значок папки со стрелкой на карточке ученика."))),
                    Block.Bullet(
                        listOf(
                            b("Убрать из списка: "),
                            t("корзина. Предложат два варианта: "),
                            p("архивировать"),
                            t(" — ученик скроется, но его история сохранится; или "),
                            n("удалить навсегда"),
                            t(" — вместе со всеми отметками."),
                        ),
                    ),
                    Block.Callout(
                        listOf(t("Если ребёнок ушёл из школы, лучше архивировать. Вернуть его можно в любой момент: «Архив» → «Восстановить».")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
            HelpCard(
                title = "Профиль ученика",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Откройте ученика из списка или подержите палец на нём во время урока. В профиле видно, "),
                            t("сколько у него хороших отметок и замечаний, какие нарушения повторяются чаще всего, "),
                            t("как дела по разным предметам и вся история записей по датам."),
                        ),
                    ),
                    Block.Bullet(listOf(b("Удалить одну запись: "), t("корзина рядом с ней."))),
                    Block.Bullet(listOf(b("Удалить все записи ученика: "), t("«Очистить всё» вверху."))),
                    Block.Bullet(listOf(b("Отчёт по одному ученику: "), t("стрелка вниз вверху экрана — например, для разговора с родителями."))),
                ),
            ),
            HelpCard(
                title = "Классы",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Классы создаются сами, когда вы загружаете учеников. Добавить вручную: "),
                            a("Мои классы → «+»"),
                            t(". Нажмите на класс — увидите учеников и их баллы, общие или по отдельному предмету."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Удаляя класс, вы удаляете и всех его учеников, и все отметки, поставленные в этом классе. "),
                            t("Перед удалением приложение покажет, сколько записей пропадёт."),
                        ),
                        CalloutKind.WARNING,
                    ),
                ),
            ),
            HelpCard(
                title = "Новый учебный год",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Летом не нужно удалять всех и загружать заново. В "),
                            a("Мои классы"),
                            t(" нажмите "),
                            b("стрелку вверх"),
                            t(": каждый класс станет на год старше, буква сохранится (5-А → 6-А)."),
                        ),
                    ),
                    Block.Bullet(listOf(b("Выпускники "), t("11-х классов (или 9-х, если школа основная) уйдут в архив или удалятся — как выберете."))),
                    Block.Bullet(listOf(b("Расписание "), t("можно сдвинуть вместе с классами одной галочкой."))),
                    Block.Bullet(listOf(b("История "), t("прошлого года не меняется: отчёты за прошлый год остаются как были."))),
                    Block.Callout(
                        listOf(
                            t("После перевода добавьте новеньких и уберите тех, кто ушёл. Попросите коллег тоже перевести классы "),
                            t("до первых отметок — иначе у завуча один и тот же ученик окажется и в 5-А, и в 6-А."),
                        ),
                        CalloutKind.TIP,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Расписание и собрания",
        cards = listOf(
            HelpCard(
                title = "Расписание",
                blocks = listOf(
                    Block.Text(listOf(t("Дни недели переключаются кнопками вверху или свайпом влево-вправо."))),
                    Block.Bullet(listOf(b("Добавить урок: "), t("«+», затем время начала, длительность (30, 35, 40 или 45 минут), предмет и класс."))),
                    Block.Bullet(listOf(b("Изменить или удалить: "), t("карандаш или корзина на карточке урока."))),
                    Block.Bullet(listOf(b("Открыть урок: "), t("нажмите на его карточку."))),
                    Block.Text(listOf(t("Если уроки пересекаются по времени, приложение предупредит."))),
                ),
            ),
            HelpCard(
                title = "Родительские собрания",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Нажмите карточку собраний на главном экране — откроется календарь. Дни с собраниями отмечены. "),
                            t("Выберите дату и нажмите «+»: тема, класс, время и заметки (например, повестка)."),
                        ),
                    ),
                    Block.Text(listOf(t("Чтобы изменить или удалить собрание, нажмите на него."))),
                ),
            ),
            HelpCard(
                title = "Напоминания",
                blocks = listOf(
                    Block.Text(listOf(t("Включаются в "), a("Настройках"), t(", в разделе «Уведомления»."))),
                    Block.Bullet(listOf(b("О собраниях "), t("— за день и за три часа."))),
                    Block.Bullet(
                        listOf(
                            b("Урок без отметок "),
                            t("— если за урок по расписанию вы не поставили ни одной отметки. Выключено по умолчанию: включайте, если часто забываете."),
                        ),
                    ),
                    Block.Bullet(listOf(b("Системные уведомления "), t("— напоминания будут появляться ещё и в шторке телефона."))),
                    Block.Callout(
                        listOf(
                            t("Напоминания проверяются, когда вы открываете приложение. Если его не открывать, напомнить оно не сможет. "),
                            t("Ночью (с 21:00 до 8:00) и в воскресенье утром приложение вас не беспокоит."),
                        ),
                        CalloutKind.INFO,
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Отчёт завучу",
        cards = listOf(
            HelpCard(
                title = "Как отправить отчёт",
                blocks = listOf(
                    Block.Step(1, listOf(t("Откройте "), a("Настройки → Отправить отчёт завучу"), t("."))),
                    Block.Step(
                        2,
                        listOf(
                            t("Выберите, что войдёт в отчёт: "),
                            b("все мои классы"),
                            t(" (по расписанию), "),
                            b("все мои отметки"),
                            t(" (всё, что есть в телефоне) или "),
                            b("один предмет"),
                            t("."),
                        ),
                    ),
                    Block.Step(
                        3,
                        listOf(
                            t("Выберите период: день, неделя, месяц или учебный год. Нужен прошлый период — выключите «текущий» и укажите дату в календаре. "),
                            t("Внизу будет видно, за какие даты попадут отметки."),
                        ),
                    ),
                    Block.Step(4, listOf(t("Нажмите "), b("«Создать отчёт»"), t(" и выберите, как передать файл."))),
                    Block.Bullet(listOf(b("«Отправить завучу по Wi-Fi» "), t("— если вы с завучем в одной школьной сети. Быстрее всего."))),
                    Block.Bullet(listOf(b("«Отправить» "), t("— через мессенджер или почту, как обычный файл."))),
                    Block.Bullet(listOf(b("«Сохранить» "), t("— файл останется в телефоне, отправите позже."))),
                ),
            ),
            HelpCard(
                title = "Как завуч объединяет отчёты",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Завуч получает отчёты от разных учителей и складывает их. Одного и того же ребёнка приложение узнаёт по "),
                            b("классу, фамилии и имени"),
                            t(". «Пупкин Вася, 5-А» у математика и «Пупкин Вася, 5-А» у географа — один ученик, "),
                            t("и его отметки по обоим предметам окажутся в одной карточке."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Регистр, пробелы, «е» и «ё», запись класса («5А» или «5-А») значения не имеют. А вот опечатка в фамилии "),
                            t("или «Вася» вместо «Василий» — это уже два разных ученика. Поэтому лучше, чтобы все загружали списки из одного места — из «Моя школа»."),
                        ),
                        CalloutKind.WARNING,
                    ),
                ),
            ),
            HelpCard(
                title = "Передача по Wi-Fi",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Удобно на совещании: файл идёт с телефона на телефон по школьной сети, без мессенджеров и интернета."),
                        ),
                    ),
                    Block.Step(1, listOf(b("Завуч"), t(" в своём режиме нажимает значок "), a("Wi-Fi"), t(" вверху, включает «Принимать отчёты» и не закрывает этот экран."))),
                    Block.Step(2, listOf(b("Учитель"), t(" создаёт отчёт, нажимает «Отправить завучу по Wi-Fi» и выбирает завуча в списке."))),
                    Block.Step(3, listOf(b("Завуч"), t(" видит, от кого пришёл файл, и нажимает «Принять». Если отчёты приходят одновременно, они встают в очередь."))),
                    Block.Callout(
                        listOf(
                            t("Завуча нет в списке? Проверьте, что оба телефона в одной сети Wi-Fi и приём включён. "),
                            t("Если всё так, а связи нет — в школьной сети, скорее всего, запрещена связь между устройствами. "),
                            t("Тогда отправьте отчёт через мессенджер."),
                        ),
                        CalloutKind.INFO,
                    ),
                ),
            ),
            HelpCard(
                title = "Про безопасность файла",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Отчёт сохраняется в формате "),
                            b(".atmo"),
                            t(": его открывает «Атмосфера», а Excel, архиваторы и просмотрщики — нет. Но это "),
                            b("не полноценная защита"),
                            t(", а внутри — фамилии детей и их отметки."),
                        ),
                    ),
                    Block.Bullet(listOf(t("Отправляйте отчёт только завучу, не в общие чаты."))),
                    Block.Bullet(listOf(t("После того как завуч загрузил файл, его можно удалить из переписки."))),
                    Block.Bullet(listOf(t("Wi-Fi-передачу используйте только в школьной сети, не в кафе и не в гостевой."))),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Для завуча",
        cards = listOf(
            HelpCard(
                title = "Как собрать отчёты учителей",
                blocks = listOf(
                    Block.Bullet(listOf(b("Файлами: "), t("нажмите стрелку вверх на главном экране завуча и выберите один или несколько файлов .atmo."))),
                    Block.Bullet(listOf(b("По Wi-Fi: "), t("значок Wi-Fi → «Принимать отчёты». После совещания приём лучше выключить."))),
                    Block.Text(
                        listOf(
                            t("Всё загруженное видно в блоке "),
                            b("«Какие отметки показывать»"),
                            t(". Любой отчёт можно временно скрыть из сводки — он не удалится, и его можно вернуть. "),
                            t("Переключатель «Мои уроки» добавляет отметки, которые вы ставили сами как учитель."),
                        ),
                    ),
                    Block.Callout(
                        listOf(t("Если учитель прислал файл повторно, ничего не задвоится: одинаковые отметки загружаются один раз.")),
                        CalloutKind.TIP,
                    ),
                ),
            ),
            HelpCard(
                title = "Что показывает сводка",
                blocks = listOf(
                    Block.Text(listOf(t("Вверху выберите период. Ниже — разделы, которые можно читать сверху вниз:"))),
                    Block.Bullet(
                        listOf(
                            b("Главные цифры. "),
                            t("Сколько было отметок, сколько из них хороших. "),
                            b("Индекс дисциплины"),
                            t(" — доля хороших минус доля замечаний: больше нуля — хороших больше."),
                        ),
                    ),
                    Block.Bullet(listOf(b("Динамика периода "), t("— становится ли лучше или хуже от недели к неделе."))),
                    Block.Bullet(listOf(b("Рейтинг классов "), t("— кто впереди, кто отстаёт. Нажмите на класс, чтобы разобрать его подробно."))),
                    Block.Bullet(listOf(b("Сравнение параллелей "), t("— например, все пятые классы рядом."))),
                    Block.Bullet(listOf(b("По классам и дням "), t("— цветная таблица: в каком классе и в какой день было тяжелее всего."))),
                    Block.Bullet(listOf(b("Ритм недели "), t("— в какие дни дети ведут себя лучше, а в какие хуже."))),
                    Block.Bullet(listOf(b("Кого поощрить и кому уделить внимание "), t("— готовые списки учеников."))),
                    Block.Bullet(listOf(b("Тезисы для педсовета "), t("— короткие выводы, которые можно взять в выступление."))),
                ),
            ),
            HelpCard(
                title = "Разбор класса и поиск ученика",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("В разборе класса есть подсказки: где коллектив силён, на каких уроках проблемы, о чём поговорить на классном часе. "),
                            t("Во вкладке «Таблица» — все ученики по предметам."),
                        ),
                    ),
                    Block.Text(listOf(t("Чтобы найти конкретного ребёнка, введите фамилию или имя в "), b("«Поиск ученика»"), t(" на главном экране завуча."))),
                ),
            ),
            HelpCard(
                title = "Выгрузить отчёт для педсовета",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Стрелка вниз есть на сводке по школе, в разборе класса и в профиле ученика. "),
                            t("Отчёт можно получить как "),
                            a("PDF"),
                            t(" — для печати и почты — или как "),
                            a("HTML"),
                            t(" — откроется в любом браузере."),
                        ),
                    ),
                ),
            ),
        ),
    ),
    HelpSection(
        title = "Ваши данные",
        cards = listOf(
            HelpCard(
                title = "Где хранятся данные",
                blocks = listOf(
                    Block.Text(
                        listOf(
                            t("Всё хранится только в памяти телефона: облака нет, разработчику ничего не уходит. "),
                            t("Наружу данные попадают, только когда вы сами отправляете отчёт."),
                        ),
                    ),
                    Block.Callout(
                        listOf(
                            t("Резервных копий в облаке Google тоже нет. Если телефон потеряется или вы удалите приложение, "),
                            b("данные пропадут"),
                            t(". Регулярно отправляйте отчёты завучу — тогда вся история хотя бы останется в школьной сводке."),
                        ),
                        CalloutKind.WARNING,
                    ),
                ),
            ),
            HelpCard(
                title = "Очистка",
                blocks = listOf(
                    Block.Text(listOf(t("В "), a("Настройки → Данные"), t(":"))),
                    Block.Bullet(listOf(b("Очистить все отметки "), t("— удалятся баллы, а классы, ученики и расписание останутся. Удобно перед новой четвертью, если историю хранить не нужно."))),
                    Block.Bullet(listOf(b("Очистить архив "), t("— удалятся архивные ученики и старые уведомления."))),
                    Block.Bullet(listOf(n("Удалить все данные "), t("— приложение станет как новое. Вернуть ничего не получится."))),
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
    var expanded by rememberSaveable(card.title) { mutableStateOf(card.expandedByDefault) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    card.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = TextSecondary,
                )
            }
            if (expanded) {
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