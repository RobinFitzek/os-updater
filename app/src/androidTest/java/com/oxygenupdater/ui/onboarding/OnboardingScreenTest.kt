package com.oxygenupdater.ui.onboarding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.ui.common.IconTextButtonTestTag
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import com.oxygenupdater.validateColumnLayout
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class OnboardingScreenTest : UsesSharedPreferencesTest() {

    @Test
    fun onboardingScreen_expanded() {
        setContent(WindowWidthSizeClass.Expanded)

        common(rule[OnboardingScreen_MainColumnTestTag].run {
            assertHasScrollAction()
            validateColumnLayout(2)
        })

        rule[OnboardingScreen_SecondaryColumnTestTag].assertHasScrollAction().validateColumnLayout(2).run {
            get(0).assertHasTextExactly(R.string.onboarding_app_uses)
            get(1).assertHasTextExactly(R.string.onboarding_caption)
        }

        rule[OnboardingScreen_DisclaimerTestTag].assertHasTextExactly(R.string.onboarding_disclaimer)
    }

    @Test
    fun onboardingScreen_compact() {
        setContent(WindowWidthSizeClass.Compact)

        val children = rule[OnboardingScreen_MainColumnTestTag].run {
            assertHasScrollAction()
            validateColumnLayout(4)
        }

        common(children)

        children[2].assertHasTextExactly(R.string.onboarding_app_uses)
        children[3].assertHasTextExactly(R.string.onboarding_caption)
        rule[OnboardingScreen_DisclaimerTestTag].assertHasTextExactly(R.string.onboarding_disclaimer)
    }

    private fun common(children: SemanticsNodeInteractionCollection) {
        // Start app button
        rule[IconTextButtonTestTag].run {
            assertHasTextExactly(R.string.onboarding_finished_button)
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onStartAppClick")
        }

        val sheet = rule[BottomSheetTestTag]

        // DeviceChooser
        children[0].run {
            assertHasTextExactly(R.string.settings_device, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // MethodChooser
        children[1].run {
            assertHasTextExactly(
                R.string.settings_update_method,
                androidx.compose.ui.R.string.not_selected,
            )

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

    }

    private fun setContent(windowWidthSize: WindowWidthSizeClass) = setContent {
        OnboardingScreen(
            windowWidthSize = windowWidthSize,
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            deviceConfig = DeviceSettingsListConfig,
            onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
            methodConfig = MethodSettingsListConfig,
            onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
            getPrefStr = ::getPrefStr,
            getPrefBool = ::getPrefBool,
            persistBool = ::persistBool,
            onStartAppClick = { trackCallback("onStartAppClick") },
        )
    }
}
