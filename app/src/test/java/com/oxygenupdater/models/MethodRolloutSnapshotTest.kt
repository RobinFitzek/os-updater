package com.oxygenupdater.models

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
