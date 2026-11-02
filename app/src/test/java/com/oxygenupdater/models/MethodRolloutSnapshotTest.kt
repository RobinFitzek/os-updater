package com.oxygenupdater.models

import com.oxygenupdater.models.MethodRolloutSnapshot.Comparison
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class MethodRolloutSnapshotTest {

    private fun snap(ota: String?, buildDate: Long = 0) = MethodRolloutSnapshot(
        methodId = 1, methodName = "Stable (full)", region = "", otaVersionNumber = ota,
        buildDate = buildDate, checkedAtEpochMs = 0,
    )

    @Test
    fun `parses EU variant code and region`() {
        val s = snap("CPH2649_17.0.0.102(EX01)", 1761800000000)
        assertEquals("EX01", s.variantCode)
        assertEquals("EU", s.regionFromVariant)
    }

    @Test
    fun `parses China, India, NA, Global variant codes`() {
        assertEquals("China", snap("CPH2653_17.0.0.110(CN01)").regionFromVariant)
        assertEquals("China", snap("CPH2653_16.0.0.860(CP10P02)").regionFromVariant)
        assertEquals("India", snap("CPH2649_17.0.0.102(IN01)").regionFromVariant)
        assertEquals("North America", snap("CPH2655_16.0.0.100(NA01)").regionFromVariant)
        assertEquals("Global", snap("CPH2747_17.0.0.106(GLO)").regionFromVariant)
        assertEquals("Global", snap("CPH2747_16.0.0.100(ALLNET)").regionFromVariant)
    }

    @Test
    fun `unknown variant code has null region`() {
        assertNull(snap("CPH2653_16.0.0.860(XX99)").regionFromVariant)
    }

    @Test
    fun `blank ota version has null variant`() {
        assertNull(snap(null).variantCode)
        assertNull(snap("").regionFromVariant)
    }

    @Test
    fun `compares by build timestamp when both have one`() {
        val current = "CPH2449_11.C.73_2730_202404251457"
        assertEquals(Comparison.Newer, snap("CPH2449_11.C.74_2740_202405101200", 202405101200).compareToCurrent(current))
        assertEquals(Comparison.Older, snap("CPH2449_11.C.72_2720_202403011200", 202403011200).compareToCurrent(current))
        assertEquals(Comparison.Same, snap(current, 202404251457).compareToCurrent(current))
    }

    @Test
    fun `falls back to numeric version compare without timestamps`() {
        val current = "CPH2649_17.0.0.102(EX01)"
        assertEquals(Comparison.Newer, snap("CPH2649_17.0.0.110(EX01)").compareToCurrent(current))
        assertEquals(Comparison.Older, snap("CPH2649_16.0.0.860(EX01)").compareToCurrent(current))
        assertEquals(Comparison.Same, snap("CPH2649_17.0.0.102(IN01)").compareToCurrent(current))
        assertEquals(Comparison.Unknown, snap(null).compareToCurrent(current))
    }

    @Test
    fun `isNewerThanCurrent uses current ota provider`() {
        val original = MethodRolloutSnapshot.currentOtaProvider
        try {
            MethodRolloutSnapshot.currentOtaProvider = { "CPH2649_17.0.0.102(EX01)" }
            assertTrue(snap("CPH2649_17.0.0.110(EX01)").isNewerThanCurrent)
            assertFalse(snap("CPH2649_16.0.0.100(EX01)").isNewerThanCurrent)
        } finally {
            MethodRolloutSnapshot.currentOtaProvider = original
        }
    }

    @Test
    fun `display version strips model, variant and timestamp`() {
        assertEquals("17.0.0.102", snap("CPH2649_17.0.0.102(EX01)").displayVersion)
        assertEquals("11.C.73", snap("CPH2449_11.C.73_2730_202404251457").displayVersion)
        assertNull(snap("").displayVersion)
    }

    @Test
    fun `parses yyyyMMddHHmm build timestamps`() {
        val millis = MethodRolloutSnapshot.buildDateMillisOf(202404251457)!!
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(2024, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.APRIL, cal.get(java.util.Calendar.MONTH))
        assertEquals(25, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(57, cal.get(java.util.Calendar.MINUTE))
        assertEquals(1761800000000, MethodRolloutSnapshot.buildDateMillisOf(1761800000000))
        assertNull(MethodRolloutSnapshot.buildDateMillisOf(0))
        assertNull(MethodRolloutSnapshot.buildDateMillisOf(2730))
    }
}
