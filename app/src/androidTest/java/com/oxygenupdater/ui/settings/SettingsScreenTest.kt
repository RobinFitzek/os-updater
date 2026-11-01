package com.oxygenupdater.ui.settings

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.get
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.IconTextButtonTestTag
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.dialogs.Themes
import com.oxygenupdater.ui.main.NavType
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest : UsesSharedPreferencesTest() {

    private val sheet = rule[BottomSheetTestTag]

    private lateinit var children: SemanticsNodeInteractionCollection

    /**
     * Only required for Android 13/T+ to ensure notification item's summary is [R.string.summary_on]
     *
     * @see deviceSection
     */
    @get:Rule
    val postNotificationsPermissionRule = GrantPostNotificationsPermissionRule()

    @Test
    fun settingsScreen() {
        setContent {
            SettingsScreen(
                navType = NavType.BottomBar,
                deviceConfig = DeviceSettingsListConfig,
                onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
                methodConfig = MethodSettingsListConfig,
                onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
                onThemeSelect = { trackCallback("onThemeSelect: $it") },
                onAdvancedModeChange = { trackCallback("onAdvancedModeChange: $it") },
                onUpdateCheckIntervalChange = { trackCallback("onUpdateCheckIntervalChange: $it") },
                openAboutScreen = { trackCallback("openAboutScreen") },
                getPrefStr = ::getPrefStr,
                getPrefBool = ::getPrefBool,
                getPrefLong = ::getPrefLong,
                persistBool = ::persistBool,
                persistLong = ::persistLong,
            )
        }

        children = rule[SettingsScreenTestTag].run {
            assertHasScrollAction()
            onChildren()
        }.also { it.assertCountEquals(15) }

        deviceSection()
        uiSection()

        // By now we must have passed more than half the children, so
        // scroll down to the last child to keep remaining ones in view.
        children.onLast().performScrollTo()

        advancedSection()
        aboutSection()
    }

    private fun deviceSection() {
        // Header
        children[0].assertHasTextExactly(R.string.preference_header_device)

        // DeviceChooser
        children[1].run {
            assertHasTextExactly(R.string.settings_device, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // MethodChooser
        children[2].run {
            assertHasTextExactly(R.string.settings_update_method, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // Background update check interval
        children[3].run {
            assertHasTextExactly(
                R.string.settings_update_check_interval,
                activity.getString(R.string.settings_update_check_interval_summary, 15L),
            )

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onUpdateCheckIntervalChange: 15")
            sheet.assertIsNotDisplayed()
        }

        // Notifications
        children[4].run {
            assertHasTextExactly(R.string.preference_header_notifications, R.string.summary_on)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }
    }

    private fun uiSection() {
        children[5].assertHasTextExactly(R.string.preference_header_ui)

        // Theme
        children[6].run {
            assertHasTextExactly(R.string.label_theme, Theme.System.titleResId)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onThemeSelect: ${Themes[0]}")
            sheet.assertIsNotDisplayed()
        }

        // Language
        children[7].run {
            assertHasTextExactly(
                R.string.label_language,
                with(currentLocale) {
                    displayName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(this) else it.toString()
                    }
                },
            )

            if (SDK_INT >= VERSION_CODES.TIRAMISU) {
                sheet.assertIsNotDisplayed() // should open system's app locale screen
                assertHasClickAction()
                // Don't performClick because that'll leave the app, which will fail subsequent tests
            } else {
                assertAndPerformClick()
                // Our own sheet is used only below Android 13
                sheet.assertIsDisplayed()
                rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[1].assertAndPerformClick()
                sheet.assertIsNotDisplayed()
            }
        }
    }

    private fun advancedSection() {
        children[8].assertHasTextExactly(R.string.preference_header_advanced)

        // Advanced mode switch
        children[9].run {
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_off)
            val switch = onChild().assertIsOff()

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule[IconTextButtonTestTag].assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("onAdvancedModeChange: true")
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_on)
            switch.assertIsOn()
        }

        // Analytics switch
        children[10].run {
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_on)
            val switch = onChild().assertIsOn()

            assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("persistBool: $KeyShareAnalyticsAndLogs=false")
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_off)
            switch.assertIsOff()
        }

        // Clear cache (conditional test, depends on cache size)
        // This is disabled if disk cache size is 0, which should be the case for standalone
        // runs of this file, but not if it's part of a full test run (CI or managed device).
        children[11].run {
            val zeroBytesText = activity.formatFileSize(0)
            val text = fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)

            // Compose M3 wraps disabled elements in a `RectangleShape`, and so text would be null here
            if (text == null) assertHasNoClickAction().onChildren().run {
                this[1].assertHasTextExactly(R.string.settings_clear_cache)
                this[2].assertHasTextExactly(zeroBytesText)
            } else assertHasClickAction().also {
                assert(text.isNotEmpty()) {
                    "Expected non-empty text for the 'Clear cache' SettingsItem"
                }
                assert(text.first().text == activity.getString(R.string.settings_clear_cache))
                assert(text.last().text != zeroBytesText)
            }
        }
    }

    private fun aboutSection() {
        children[12].assertHasTextExactly(R.string.preference_header_about)

        // Privacy policy
        children[13].run {
            assertHasTextExactly(R.string.label_privacy_policy, R.string.summary_privacy_policy)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // Rate app
        children[14].run {
            assertHasTextExactly(R.string.label_rate_app, R.string.summary_rate_app)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // App version
        children[15].run {
            assertHasTextExactly(R.string.app_name, "v${BuildConfig.VERSION_NAME}")
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("openAboutScreen")
        }
    }
}
