/**
 * Отступы контента под edge-to-edge: системная навигация не перекрывает прокручиваемые экраны.
 */
package ru.golpom.atmosphere.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun navigationBarBottomInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Нижний отступ списка с учётом системной навигации. */
@Composable
fun listBottomPadding(extra: Dp = 16.dp): PaddingValues =
    PaddingValues(bottom = extra + navigationBarBottomInset())

/** Нижний отступ списка над FAB и системной навигацией. */
@Composable
fun fabListBottomPadding(fabClearance: Dp = 80.dp): PaddingValues =
    PaddingValues(bottom = fabClearance + navigationBarBottomInset())

/** Завершает прокручиваемый экран — контент можно долистать ниже nav bar. */
@Composable
fun NavigationBarScrollSpacer(extra: Dp = 16.dp) {
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    if (extra > 0.dp) {
        Spacer(Modifier.height(extra))
    }
}
