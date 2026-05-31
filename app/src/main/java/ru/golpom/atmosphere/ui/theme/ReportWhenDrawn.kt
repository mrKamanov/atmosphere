/**
 * Сигнал «первый кадр отрисован» — для удержания системного splash до готовности UI.
 */
package ru.golpom.atmosphere.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun Modifier.reportWhenDrawn(onReady: () -> Unit): Modifier {
    val reported = remember { AtomicBoolean(false) }
    return onGloballyPositioned {
        if (reported.compareAndSet(false, true)) {
            onReady()
        }
    }
}
