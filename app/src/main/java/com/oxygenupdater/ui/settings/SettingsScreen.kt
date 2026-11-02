package com.oxygenupdater.ui.settings

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.imageLoader
import com.oxygenupdater.ui.AppLocales
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openAppLocalePage
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.icons.Check
import com.oxygenupdater.icons.CloudDownload
import com.oxygenupdater.icons.DeleteSweep
import com.oxygenupdater.icons.Language
import com.oxygenupdater.icons.LockOpen
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.Mobile
import com.oxygenupdater.icons.Notifications
import com.oxygenupdater.icons.Palette
import com.oxygenupdater.icons.Schedule
import com.oxygenupdater.icons.Policy
import com.oxygenupdater.icons.Star
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyUpdateCheckIntervalMinutes
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateCheckIntervalOption
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.ui.dialogs.LanguageSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.dialogs.ThemeSheet
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.LocalTheme
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.DefaultUpdateCheckIntervalMinutes
import com.oxygenupdater.utils.MinUpdateCheckIntervalMinutes
import com.oxygenupdater.utils.NotifStatus
import com.oxygenupdater.utils.NotifUtils
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.okHttpCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navType: NavType,
    deviceConfig: SettingsListConfig<Device>,
    onDeviceSelect: (Device) -> Unit,
    methodConfig: SettingsListConfig<UpdateMethod>,
    onMethodSelect: (UpdateMethod) -> Unit,
    onThemeSelect: (Theme) -> Unit,
    onAdvancedModeChange: (Boolean) -> Unit,
    onUpdateCheckIntervalChange: (Long) -> Unit,
    deviceListLoadFailed: Boolean = false,
    onRetryLoadDevices: () -> Unit = {},
    openAboutScreen: () -> Unit,
    getPrefStr: (key: String, default: String) -> String,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    getPrefLong: (key: String, default: Long) -> Long,
    persistBool: (key: String, value: Boolean) -> Unit,
    persistLong: (key: String, value: Long) -> Unit,
) = Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .testTag(SettingsScreenTestTag)
) {
    // region Device
    Header(R.string.preference_header_device)

    DeviceChooser(
        config = deviceConfig,
        getPrefStr = getPrefStr,
        onSelect = onDeviceSelect,
        loadFailed = deviceListLoadFailed,
        onRetry = onRetryLoadDevices,
    )
    MethodChooser(
        config = methodConfig,
        getPrefStr = getPrefStr,
        onSelect = onMethodSelect,
        loadFailed = deviceListLoadFailed,
        onRetry = onRetryLoadDevices,
    )

    UpdateCheckInterval(
        getPrefLong = getPrefLong,
        persistLong = persistLong,
        onIntervalChange = onUpdateCheckIntervalChange,
    )
    Notifications()
    // endregion

    // region UI
    Header(R.string.preference_header_ui)
    Theme(onSelect = onThemeSelect)
    Language()
    // endregion

    // region Advanced
    Header(R.string.preference_header_advanced)
    AdvancedMode(getPrefBool = getPrefBool, onChange = onAdvancedModeChange)
    ClearCache()
    // endregion

    // region About
    Header(R.string.preference_header_about)

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    SettingsItem(
        // Use Chrome Custom Tabs to open the privacy policy link
        onClick = { customTabIntent.launch(context, "https://oxygenupdater.com/privacy/") },
        icon = Symbols.Policy,
        titleResId = R.string.label_privacy_policy,
        subtitle = stringResource(R.string.summary_privacy_policy),
    )

    SettingsItem(
        onClick = context::openPlayStorePage,
        icon = Symbols.Star,
        titleResId = R.string.label_rate_app,
        subtitle = stringResource(R.string.summary_rate_app),
    )

    SettingsItem(
        onClick = openAboutScreen,
        icon = Logos.LogoNotification,
        titleResId = R.string.app_name,
        subtitle = "v${BuildConfig.VERSION_NAME}",
    )
    // endregion

    ConditionalNavBarPadding(navType)
}

@Composable
fun DeviceChooser(
    config: SettingsListConfig<Device>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (Device) -> Unit,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    var showSheet by rememberState(false)

    val deviceSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (deviceSelectionEnabled) {
        getPrefStr(KeyDevice, notSelected)
    } else if (loadFailed) {
        stringResource(R.string.summary_device_list_load_failed)
    } else stringResource(R.string.summary_please_wait)

    SettingsItem(
        onClick = { if (loadFailed) onRetry() else showSheet = true },
        icon = Symbols.Mobile,
        titleResId = R.string.settings_device,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected || loadFailed,
        enabled = true, // tappable even on failure, so the user can retry
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = config,
            titleResId = R.string.settings_search_devices,
            captionResId = R.string.onboarding_device_chooser_caption,
            filter = DeviceSearchFilter,
            onClick = onSelect,
        )
    }
}

@Composable
fun MethodChooser(
    config: SettingsListConfig<UpdateMethod>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (UpdateMethod) -> Unit,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    var showSheet by rememberState(false)

    val methodSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (methodSelectionEnabled) {
        getPrefStr(KeyUpdateMethod, notSelected)
    } else if (loadFailed) {
        stringResource(R.string.summary_device_list_load_failed)
    } else stringResource(R.string.summary_update_method)

    SettingsItem(
        onClick = { if (loadFailed) onRetry() else showSheet = true },
        icon = Symbols.CloudDownload,
        titleResId = R.string.settings_update_method,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected || loadFailed,
        enabled = true,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = config,
            titleResId = R.string.settings_update_method,
            captionResId = R.string.onboarding_method_chooser_caption,
            onClick = onSelect,
        )
    }
}

@Composable
private fun UpdateCheckInterval(
    getPrefLong: (key: String, default: Long) -> Long,
    persistLong: (key: String, value: Long) -> Unit,
    onIntervalChange: (Long) -> Unit,
) {
    var showSheet by rememberState(false)

    val intervals = remember { listOf(1L, 2L, 5L, 10L, 15L, 30L, 60L, 120L, 360L, 720L, 1440L) }
    val current = getPrefLong(KeyUpdateCheckIntervalMinutes, DefaultUpdateCheckIntervalMinutes)
        .coerceAtLeast(MinUpdateCheckIntervalMinutes)
    val subtitle = stringResource(R.string.settings_update_check_interval_summary, current)

    SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.Schedule,
        titleResId = R.string.settings_update_check_interval,
        subtitle = subtitle,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = SettingsListConfig(
                list = intervals.map { UpdateCheckIntervalOption(it) },
                recommendedId = DefaultUpdateCheckIntervalMinutes,
                selectedId = current,
            ),
            titleResId = R.string.settings_update_check_interval,
            captionResId = R.string.settings_update_check_interval_caption,
            onClick = {
                persistLong(KeyUpdateCheckIntervalMinutes, it.id)
                onIntervalChange(it.id)
                hide()
            },
        )
    }
}

@Composable
private fun Notifications() {
    val context = LocalContext.current
    val runningInPreview = LocalInspectionMode.current
    var notifStatus by rememberState(if (runningInPreview) NotifStatus() else NotifUtils.toNotifStatus(context))

    if (!runningInPreview) LifecycleResumeEffect(context) {
        // Check again in case user changed settings and came back to the app
        notifStatus = NotifUtils.toNotifStatus(context)
        onPauseOrDispose {}
    }

    val disabled = notifStatus.disabled
    val subtitleIsError: Boolean
    val notifSummary = if (disabled.isNullOrEmpty()) {
        subtitleIsError = true
        stringResource(R.string.summary_off)
    } else if (disabled.none { it }) {
        subtitleIsError = false
        stringResource(R.string.summary_on)
    } else stringResource(R.string.summary_important_notifications_disabled) + buildString {
        disabled.forEachIndexed { index, flag ->
            if (!flag) return@forEachIndexed
            when (index) {
                0 -> R.string.update_notification_channel_name
                1 -> R.string.news_notification_channel_name
                2 -> R.string.download_status_notification_channel_name
                else -> throw ArrayIndexOutOfBoundsException(index)
            }.let { append("\n• " + stringResource(it)) }
        }
    }.also {
        subtitleIsError = true
    }

    SettingsItem(
        onClick = {
            val packageName = context.packageName
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            // Works only for API 21+ (Lollipop), which happens to be the min API
            else Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra("app_package", packageName).putExtra("app_uid", context.applicationInfo.uid)
            context.startActivity(intent)
        },
        icon = Symbols.Notifications,
        titleResId = R.string.preference_header_notifications,
        subtitle = notifSummary,
        subtitleIsError = subtitleIsError,
    )
}

@Composable
private fun Theme(onSelect: (Theme) -> Unit) {
    var showSheet by rememberState(false)
    SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.Palette,
        titleResId = R.string.label_theme,
        subtitle = stringResource(LocalTheme.current.titleResId),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        ThemeSheet {
            onSelect(it)
            hide()
        }
    }
}

@Composable
private fun Language() {
    val selectedLocale = currentLocale()
    val language = remember(selectedLocale) {
        selectedLocale.displayName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(selectedLocale) else it.toString()
        }
    }

    var showSheet by rememberState(false)
    val context = LocalContext.current
    SettingsItem(
        onClick = {
            // Use our own sheet below Android 13
            showSheet = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true else try {
                // Otherwise delegate to system API on Android 13+ if possible
                context.openAppLocalePage()
                false
            } catch (e: Exception) {
                logInfo("SettingsScreen", "openAppLocalePage failed", e)
                true // fallback just in case
            }
        },
        icon = Symbols.Language,
        titleResId = R.string.label_language,
        subtitle = language,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        LanguageSheet({
            (context as? Activity)?.let { activity -> AppLocales.set(activity, it) }
            hide()
        }, selectedLocale)
    }
}

@Composable
private fun AdvancedMode(
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    onChange: (Boolean) -> Unit,
) {
    var showSheet by rememberState(false)

    // Maintain state if user leaves this screen, then comes back to it
    var advancedMode by rememberState(getPrefBool(KeyAdvancedMode, false))
    SettingsSwitchItem(
        checked = advancedMode,
        onCheckedChange = {
            onChange(it)
            advancedMode = it
        },
        icon = Symbols.LockOpen,
        titleResId = R.string.settings_advanced_mode,
        showWarning = { showSheet = true },
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        AdvancedModeSheet {
            onChange(it)
            advancedMode = it
            hide()
        }
    }
}

@Composable
private fun ClearCache() {
    val context = LocalContext.current
    val coilImageLoader = context.imageLoader

    var recomputationKey by rememberState(false)
    var size by rememberState(
        recomputationKey,
        try {
            // Only count disk cache sizes
            (okHttpCache?.size() ?: 0L) + (coilImageLoader.diskCache?.size ?: 0L)
        } catch (_: Exception) {
            0L
        }
    )

    // For running IO-intensive operations
    val scope = rememberCoroutineScope { Dispatchers.IO }
    SettingsItem(
        onClick = {
            try {
                /**
                 * https://square.github.io/okhttp/features/caching/#pruning-the-cache
                 * We choose to only clear cache for API calls and images. Other caches, e.g. WebView
                 * are not deleted. If this decision changes in the future, call `okHttpCache?.delete()`
                 * to clear everything in the cache directory. For posterity, WebView cache can be
                 * programmatically cleared via `WebView(context).clearCache(true)`.
                 */
                scope.launch {
                    okHttpCache?.evictAll()
                    coilImageLoader.diskCache?.clear()
                    // coilImageLoader.memoryCache?.clear() // is this required?
                }

                // Toggle to let the `size` calculation run again
                recomputationKey = !recomputationKey

                context.showToast(android.R.string.ok)
            } catch (e: Exception) {
                logInfo(SettingsScreenTestTag, "Error when clearing cache", e)
            }
        },
        icon = Symbols.DeleteSweep,
        titleResId = R.string.settings_clear_cache,
        subtitle = context.formatFileSize(size),
        enabled = size != 0L,
    )
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    showWarning: (() -> Unit)? = null,
) {
    val checkedChange by rememberUpdatedState<(checked: Boolean) -> Unit> {
        // Handoff to BottomSheet if necessary, pref will update based on user choice (Cancel/Enable)
        if (it && showWarning != null) showWarning() else {
            onCheckedChange(it)
        }
    }

    SettingsItem(
        onClick = { checkedChange(!checked) },
        icon = icon,
        titleResId = titleResId,
        subtitle = stringResource(if (checked) R.string.summary_on else R.string.summary_off),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = checkedChange,
            thumbContent = {
                if (!checked) return@Switch
                Icon(Symbols.Check, null, Modifier.size(SwitchDefaults.IconSize))
            },
            modifier = Modifier.windowInsetsPadding(
                // Leave space for 2/3-button nav bar in landscape mode
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
        )
    }
}

@Composable
@NonRestartableComposable
private fun Header(@StringRes textResId: Int) {
    HorizontalDivider()
    Text(
        text = stringResource(textResId),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifierDefaultPaddingStartTopEnd
    )
}

@Composable
@NonRestartableComposable
fun SettingsItem(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    subtitle: String?,
    subtitleIsError: Boolean = false,
    enabled: Boolean = true,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifierMaxWidth
        .alpha(if (enabled) 1f else 0.38f)
        .animatedClickable(enabled, onClick)
        .then(modifierDefaultPadding) // must be after `clickable`
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Icon(icon, stringResource(R.string.icon), tint = colorScheme.primary)

    Column(
        Modifier
            .weight(1f)
            .padding(start = 16.dp, end = if (content != null) 16.dp else 0.dp)
    ) {
        Text(
            text = stringResource(titleResId),
            style = typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )

        if (subtitle != null) Text(
            text = subtitle,
            color = if (subtitleIsError) colorScheme.error else colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis, maxLines = 10,
            style = typography.bodyMedium,
        )
    }

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

@VisibleForTesting
val DeviceSearchFilter: (Device, String) -> Boolean = { device, query ->
    device.name?.contains(query, ignoreCase = true) == true
            || device.productNames.any { it.contains(query, ignoreCase = true) }
}

@VisibleForTesting
const val SettingsScreenTestTag = "SettingsScreen"

@VisibleForTesting
val DeviceSettingsListConfig = SettingsListConfig(
    list = listOf(
        Device(
            id = 1,
            name = "OnePlus 7 Pro",
            productNames = listOf("OnePlus7Pro,GM1917"),
            enabled = true,
        ),
        Device(
            id = 2,
            name = "OnePlus 8T",
            productNames = listOf("OnePlus8T,KB2001"),
            enabled = true,
        ),
    ),
    recommendedId = 2,
    selectedId = 1,
)

@VisibleForTesting
val MethodSettingsListConfig = SettingsListConfig(
    list = listOf(
        UpdateMethod(
            id = 1,
            name = "Stable (full)",
            recommendedForRootedDevice = true,
            recommendedForNonRootedDevice = false,
            supportsRootedDevice = true,
        ),
        UpdateMethod(
            id = 2,
            name = "Stable (incremental)",
            recommendedForRootedDevice = false,
            recommendedForNonRootedDevice = true,
            supportsRootedDevice = false,
        )
    ),
    recommendedId = 2,
    selectedId = 1,
)

@PreviewThemes
@Composable
fun PreviewSettingsScreen() = PreviewAppTheme {
    SettingsScreen(
        navType = NavType.BottomBar,
        deviceConfig = DeviceSettingsListConfig,
        onDeviceSelect = {},
        methodConfig = MethodSettingsListConfig,
        onMethodSelect = {},
        onThemeSelect = {},
        onAdvancedModeChange = {},
        onUpdateCheckIntervalChange = {},
        openAboutScreen = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        getPrefLong = { _, default -> default },
        persistBool = { _, _ -> },
        persistLong = { _, _ -> },
    )
}
