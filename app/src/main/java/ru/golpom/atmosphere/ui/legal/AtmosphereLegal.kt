/**
 * Публичные юридические ссылки приложения.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

object AtmosphereLegal {
    const val PRIVACY_POLICY_URL =
        "https://github.com/mrKamanov/atmosphere/blob/main/docs/PRIVACY_POLICY.md"
}

fun Context.openPrivacyPolicy() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AtmosphereLegal.PRIVACY_POLICY_URL))
    ContextCompat.startActivity(this, intent, null)
}
