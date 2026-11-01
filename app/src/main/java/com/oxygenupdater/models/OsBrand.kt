package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting

/**
 * OS branding/era helpers.
 *
 * OnePlus retired OxygenOS with Android 17: from ColorOS 17 onwards there is no
 * "OxygenOS 17" — OnePlus devices are unified under ColorOS (same as OPPO/realme).
 * OxygenOS 16 is the last OxygenOS major version; devices can stay on it (security
 * patches continue) or voluntarily move to ColorOS 17.
 *
 * Version strings look like:
 * - OxygenOS: `OxygenOS 15.0.0.850(EX01)` / OTA version `CPH2653_16.0.0.860(EX01)`
 * - ColorOS:  `ColorOS 17.0.0.102(EX01)`  / OTA version `CPH2649_17.0.0.102(EX01)`
 */
object OsBrand {

    private val OsNameVersionPattern = Regex(
        """(OxygenOS|ColorOS|HydrogenOS|realme UI)\s*(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    /** Variant code is the parenthesized suffix of an OTA version, e.g. `EX01` in `CPH2649_17.0.0.102(EX01)` */
    private val VariantCodePattern = Regex("""\(([A-Z0-9]+)\)\s*$""")

    data class Info(
        val name: String,
        val major: Int,
        val variantCode: String?,
    ) {
        /** Prefix used when formatting version numbers, e.g. `"OxygenOS "` (trailing space kept) */
        val prefix: String = "$name "

        /**
         * `true` in the unified ColorOS era: either explicitly ColorOS, or major >= 17
         * (OnePlus has no OxygenOS 17 — Android 17+ on OnePlus is ColorOS 17+).
         */
        val isColorOsEra: Boolean
            get() = name.equals("ColorOS", ignoreCase = true) ||
                    name.equals("HydrogenOS", ignoreCase = true) ||
                    major >= 17

        /** Human-readable region for [variantCode] */
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
    }

    /**
     * Parses an OS version string (marketing version like `OxygenOS 15.0.0.850(EX01)`,
     * or an OTA version like `CPH2653_16.0.0.860(EX01)`).
     */
    @VisibleForTesting
    fun parse(version: String?): Info? {
        if (version.isNullOrBlank()) return null
        val nameMatch = OsNameVersionPattern.find(version)
        val variantCode = VariantCodePattern.find(version)?.groupValues?.get(1)

        val name: String
        val major: Int
        if (nameMatch != null) {
            name = nameMatch.groupValues[1].replaceFirstChar { it.titlecase() }
            major = nameMatch.groupValues[2].toIntOrNull() ?: 0
        } else {
            // OTA versions like `CPH2649_17.0.0.102(EX01)` carry the major after the model prefix
            val otaMajor = Regex("""_[A-Z0-9]+_(\d+)\.\d+""", RegexOption.IGNORE_CASE)
                .find(version)?.groupValues?.get(1)?.toIntOrNull()
            if (otaMajor == null) return null
            name = "OS"
            major = otaMajor
        }

        return Info(name = name, major = major, variantCode = variantCode)
    }

    /** Brand/era of the OS currently installed on this device */
    val current: Info
        get() = parse(SystemVersionProperties.osVersion)
            ?: parse(SystemVersionProperties.otaVersion)
            ?: Info(
                name = when (SystemVersionProperties.brandLowercase) {
                    "oppo" -> "ColorOS"
                    "realme" -> "realme UI"
                    else -> "OxygenOS"
                },
                major = 0,
                variantCode = VariantCodePattern.find(SystemVersionProperties.otaVersion)
                    ?.groupValues?.get(1),
            )
}
