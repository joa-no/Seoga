package com.joano.seoga.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.joano.seoga.R
import java.util.Locale

object LocalizationUtil {

    /**
     * Resolves the java.util.Locale based on user preference:
     * - "auto": Follows device locale (pt -> pt-BR, es -> es, en -> en, others -> en)
     * - "pt-BR": Portuguese (Brazil)
     * - "es": Spanish
     * - "en": English
     */
    fun resolveLocale(preference: String): Locale {
        return when (preference) {
            "pt-BR", "pt" -> Locale("pt", "BR")
            "es" -> Locale("es")
            "en" -> Locale("en")
            else -> {
                val sys = Locale.getDefault()
                when (sys.language.lowercase()) {
                    "pt" -> Locale("pt", "BR")
                    "es" -> Locale("es")
                    "en" -> Locale("en")
                    else -> Locale("en") // Requested rule: other languages fallback to English
                }
            }
        }
    }

    /**
     * Resolves effective language tag for external metadata searches: "pt-BR", "es", or "en".
     * Never falls back to arbitrary device locales.
     */
    fun resolveEffectiveLanguageTag(preference: String): String {
        val locale = resolveLocale(preference)
        return when (locale.language.lowercase()) {
            "pt" -> "pt-BR"
            "es" -> "es"
            else -> "en"
        }
    }

    /**
     * Translates the canonical internal work type ("Mangá", "Manhwa", "Manhua", "HQ")
     * into the currently selected display language.
     */
    @Composable
    fun getLocalizedWorkType(type: String): String {
        return when (type) {
            "Mangá", "Manga" -> stringResource(R.string.type_manga)
            "Manhwa" -> stringResource(R.string.type_manhwa)
            "Manhua" -> stringResource(R.string.type_manhua)
            "HQ", "Comic", "Quadrinho" -> stringResource(R.string.type_comic)
            "Todos", "All" -> stringResource(R.string.type_all)
            else -> type
        }
    }

    fun getLocalizedWorkType(context: Context, type: String): String {
        return when (type) {
            "Mangá", "Manga" -> context.getString(R.string.type_manga)
            "Manhwa" -> context.getString(R.string.type_manhwa)
            "Manhua" -> context.getString(R.string.type_manhua)
            "HQ", "Comic", "Quadrinho" -> context.getString(R.string.type_comic)
            "Todos", "All" -> context.getString(R.string.type_all)
            else -> type
        }
    }

    /**
     * Canonical types mapped to their string resource IDs for filter chips.
     */
    val canonicalTypesWithRes = listOf(
        null to R.string.type_all,
        "Mangá" to R.string.type_manga,
        "Manhwa" to R.string.type_manhwa,
        "Manhua" to R.string.type_manhua,
        "HQ" to R.string.type_comic
    )
}
