package com.oxygenupdater.models

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class MethodRolloutSnapshotTest {

    private fun snap(ota: String?, buildDate: Long = 0) = MethodRolloutSnapshot(
        methodId = 1, methodName = "Stable (full)", otaVersionNumber = ota,
        buildDate = buildDate, checkedAtEpochMs = 0,
    )

    @Test
    fun `parses EU variant code and region`() {
        val s = snap("CPH2649_17.0.0.102(EX01)", 1761800000000)
        assertEquals("EX01", s.variantCode)
        assertEquals("EU", s.region)
    }

    @Test
    fun `parses China, India, NA, Global variant codes`() {
        assertEquals("China", snap("CPH2653_17.0.0.110(CN01)").region)
        assertEquals("China", snap("CPH2653_16.0.0.860(CP10P02)").region)
        assertEquals("India", snap("CPH2649_17.0.0.102(IN01)").region)
        assertEquals("North America", snap("CPH2655_16.0.0.100(NA01)").region)
        assertEquals("Global", snap("CPH2747_17.0.0.106(GLO)").region)
        assertEquals("Global", snap("CPH2747_16.0.0.100(ALLNET)").region)
    }

    @Test
    fun `unknown variant code has null region`() {
        assertNull(snap("CPH2653_16.0.0.860(XX99)").region)
    }

    @Test
    fun `blank ota version has null variant`() {
        assertNull(snap(null).variantCode)
        assertNull(snap("").region)
    }

    @Test
    fun `isNewerThanCurrent compares build dates`() {
        val original = MethodRolloutSnapshot.currentBuildDateProvider
        try {
            MethodRolloutSnapshot.currentBuildDateProvider = { 1000L }
            assertTrue(snap("CPH2649_17.0.0.102(EX01)", 1761800000000).isNewerThanCurrent)
            assertFalse(snap("CPH2649_16.0.0.100(EX01)", 0).isNewerThanCurrent)
        } finally {
            MethodRolloutSnapshot.currentBuildDateProvider = original
        }
    }
}
