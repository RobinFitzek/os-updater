package com.oxygenupdater.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

val currentLocale: Locale
    get() = AppLocales.get() ?: LocaleListCompat.getAdjustedDefault()[0]!!

/**
 * Per-app language without AppCompat: the platform [LocaleManager] on Android 13+, and on older
 * versions a stored language tag applied to the activity via [wrap] (what AppCompat's backport did).
 */
object AppLocales {

    private const val PrefsName = "app_locales"
    private const val KeyTag = "language_tag"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Locale? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        appContext.getSystemService(LocaleManager::class.java).applicationLocales[0]
    } else storedTag(appContext)?.let(Locale::forLanguageTag)

    /** @param tag BCP 47 language tag, or empty to follow the system language */
    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit { putString(KeyTag, tag) }
            activity.recreate()
        }
    }

    /** Call from [Activity.attachBaseContext]; no-op on Android 13+ where the platform handles it */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = storedTag(base)?.let(Locale::forLanguageTag) ?: return base
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun storedTag(context: Context) = context
        .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getString(KeyTag, null)
        ?.ifEmpty { null }
}

/**
 * We send full language tags only if the language in question is region-qualified in the app.
 * This avoids polluting cache with unnecessary language variant info. For example, there will
 * always be only one "English" or "Dutch" supported in the app.
 */
inline val currentLanguage: String
    get() = currentLocale.languageOrLanguageTag()

@Composable
@ReadOnlyComposable
fun currentLocale() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: LocaleListCompat.getAdjustedDefault()[0]!!

/**
 * This is intentionally simple for performance.
 *
 * Its behaviour is fully-tested in [com.oxygenupdater.ui.CurrentLocaleTest].
 */
@VisibleForTesting
inline fun Locale.languageOrLanguageTag() = language.let {
    if (it == "pt" || it == "zh") toLanguageTag() else it
} ?: "en" // fallback just in case
