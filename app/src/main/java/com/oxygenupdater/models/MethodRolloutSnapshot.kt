package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable

/**
 * Client-side snapshot of what the background update check saw for one update
 * method of the selected device. Persisted as JSON in SharedPreferences, so no
 * backend endpoint is required to show a per-method rollout overview.
 *
 * [otaVersionNumber] embeds the variant code (e.g. `CPH2649_17.0.0.102(EX01)`)
 * and the build timestamp (trailing number after the last `_`).
 */
@Serializable
data class MethodRolloutSnapshot(
    val methodId: Long,
    val methodName: String?,
    val otaVersionNumber: String?,
    val buildDate: Long,
    val checkedAtEpochMs: Long,
) {
    val variantCode: String?
        get() = otaVersionNumber?.let { Regex("""\(([A-Z0-9]+)\)\s*$""").find(it)?.groupValues?.get(1) }

    val region: String?
        get() = variantCode?.let { code ->
            when {
                code.startsWith("EX") -> "EU"
                code.startsWith("CN") || code.startsWith("CP") -> "China"
                code.startsWith("IN") -> "India"
                code.startsWith("NA") -> "North America"
                code.startsWith("GLO") || code.startsWith("ALLNET") -> "Global"
                else -> null
            }
        }

    val isNewerThanCurrent: Boolean
        get() = buildDate > UpdateData.getBuildDate(SystemVersionProperties.otaVersion)

    companion object {
        @VisibleForTesting
        val Empty = MethodRolloutSnapshot(0, null, null, 0, 0)
    }
}
