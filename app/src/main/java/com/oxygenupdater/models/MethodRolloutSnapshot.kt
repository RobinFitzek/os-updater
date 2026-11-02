package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable
import java.util.Calendar

/**
 * Client-side snapshot of what the background update check saw for one update
 * method of the selected device. Persisted as JSON in SharedPreferences, so no
 * backend endpoint is required to show a per-method rollout overview.
 *
 * [otaVersionNumber] embeds the variant code (e.g. `CPH2649_17.0.0.102(EX01)`)
 * and, on older builds, the build timestamp (trailing `yyyyMMddHHmm` after the last `_`).
 */
@Serializable
data class MethodRolloutSnapshot(
    val methodId: Long,
    val methodName: String?,
    /** Region this row was probed with: EU / IN / CN / GLO / NA / "" (device's own) */
    val region: String = "",
    val otaVersionNumber: String?,
    val buildDate: Long,
    val checkedAtEpochMs: Long,
    /**
     * When this app first saw [otaVersionNumber] in this method × region, or `0` if it was
     * already there when tracking started (see [RolloutFirstSeen])
     */
    val firstSeenAtEpochMs: Long = 0,
) {
    val variantCode: String?
        get() = otaVersionNumber?.let { VariantRegex.find(it)?.groupValues?.get(1) }

    /** Region derived from the variant code in [otaVersionNumber] */
    val regionFromVariant: String?
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

    /** Human-readable version without model prefix, variant or timestamp, e.g. `17.0.0.102` */
    val displayVersion: String?
        get() = displayVersionOf(otaVersionNumber)

    /** [buildDate] as epoch millis, or `null` if [otaVersionNumber] doesn't carry a usable timestamp */
    val buildDateMillis: Long?
        get() = buildDateMillisOf(buildDate)

    val isNewerThanCurrent: Boolean
        get() = compareToCurrent() == Comparison.Newer

    /**
     * Compares against the build installed on this device: by build timestamp if both
     * have one, otherwise by the numeric parts of [displayVersion].
     */
    fun compareToCurrent(currentOta: String = currentOtaProvider()): Comparison {
        val ota = otaVersionNumber
        if (ota.isNullOrBlank()) return Comparison.Unknown
        if (ota == currentOta) return Comparison.Same

        val currentDate = UpdateData.getBuildDate(currentOta)
        val result = if (buildDate > 0 && currentDate > 0) {
            buildDate.compareTo(currentDate)
        } else compareVersions(displayVersion, displayVersionOf(currentOta)) ?: return Comparison.Unknown

        return when {
            result > 0 -> Comparison.Newer
            result < 0 -> Comparison.Older
            else -> Comparison.Same
        }
    }

    enum class Comparison { Newer, Same, Older, Unknown }

    /** Key in the persisted maps: one entry per method × region */
    val key: String
        get() = "$methodId:$region"

    companion object {
        @VisibleForTesting
        val Empty = MethodRolloutSnapshot(0, null, "", null, 0, 0)

        /** Regions probed for the rollout table, in display order */
        val RolloutRegions = listOf("EU", "IN", "CN", "GLO", "NA")

        private val VariantRegex = Regex("""\(([A-Z0-9]+)\)\s*$""")

        /** Overridable in tests; defaults to the device's current OTA version */
        var currentOtaProvider: () -> String = { SystemVersionProperties.otaVersion }

        fun displayVersionOf(ota: String?): String? {
            if (ota.isNullOrBlank()) return null
            return ota.substringAfter('_', ota)
                .replace(VariantRegex, "")
                .substringBefore('_')
                .trim()
                .ifEmpty { null }
        }

        /** Supports both `yyyyMMddHHmm` OTA timestamps and plain epoch millis */
        fun buildDateMillisOf(buildDate: Long): Long? {
            if (buildDate <= 0) return null
            val digits = buildDate.toString()
            if (digits.length == 12 && digits.startsWith("20")) {
                val year = digits.substring(0, 4).toInt()
                val month = digits.substring(4, 6).toInt()
                val day = digits.substring(6, 8).toInt()
                val hour = digits.substring(8, 10).toInt()
                val minute = digits.substring(10, 12).toInt()
                if (month !in 1..12 || day !in 1..31 || hour > 23 || minute > 59) return null
                return Calendar.getInstance().apply {
                    clear()
                    set(year, month - 1, day, hour, minute)
                }.timeInMillis
            }
            // Epoch millis between 2001 and 2286
            return if (digits.length == 13) buildDate else null
        }

        /** Compares dot-separated versions numerically, or `null` if either isn't comparable */
        fun compareVersions(a: String?, b: String?): Int? {
            val left = a?.split('.')?.map { it.toIntOrNull() ?: return null } ?: return null
            val right = b?.split('.')?.map { it.toIntOrNull() ?: return null } ?: return null
            for (i in 0 until maxOf(left.size, right.size)) {
                val cmp = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}

/**
 * Persisted separately from the snapshots so a failed probe never loses history.
 *
 * @param atEpochMs when [otaVersionNumber] was first seen; `0` if it was already
 *   present on the very first check of this method × region (real date unknown)
 */
@Serializable
data class RolloutFirstSeen(
    val otaVersionNumber: String?,
    val atEpochMs: Long,
) {
    companion object {
        /** Returns the entry to store for a probe that saw [ota] at [checkedAt], given the [previous] entry */
        fun next(previous: RolloutFirstSeen?, ota: String?, checkedAt: Long) = when {
            previous == null -> RolloutFirstSeen(ota, 0) // baseline: we don't know when it arrived
            ota.isNullOrBlank() || previous.otaVersionNumber == ota -> previous
            else -> RolloutFirstSeen(ota, checkedAt)
        }
    }
}
