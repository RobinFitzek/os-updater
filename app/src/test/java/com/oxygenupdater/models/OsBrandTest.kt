package com.oxygenupdater.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsBrandTest {

    @Test
    fun `parses OxygenOS marketing version with EU variant`() {
        val info = OsBrand.parse("OxygenOS 15.0.0.850(EX01)")!!
        assertEquals("OxygenOS", info.name)
        assertEquals(15, info.major)
        assertEquals("EX01", info.variantCode)
        assertEquals("EU", info.region)
        assertFalse(info.isColorOsEra)
        assertEquals("OxygenOS ", info.prefix)
    }

    @Test
    fun `parses ColorOS 17 marketing version with India variant`() {
        val info = OsBrand.parse("ColorOS 17.0.0.102(IN01)")!!
        assertEquals("ColorOS", info.name)
        assertEquals(17, info.major)
        assertTrue(info.isColorOsEra)
        assertEquals("India", info.region)
    }

    @Test
    fun `parses OTA version without OS name using major after model prefix`() {
        val info = OsBrand.parse("CPH2649_17.0.0.102(EX01)")!!
        assertEquals(17, info.major)
        assertEquals("EX01", info.variantCode)
        // No OxygenOS 17 exists: major >= 17 means unified ColorOS era
        assertTrue(info.isColorOsEra)
    }

    @Test
    fun `OxygenOS 16 OTA version is not ColorOS era`() {
        val info = OsBrand.parse("CPH2653_16.0.0.860(CN01)")!!
        assertEquals(16, info.major)
        assertFalse(info.isColorOsEra)
        assertEquals("China", info.region)
    }

    @Test
    fun `global variant maps to Global`() {
        assertEquals("Global", OsBrand.parse("CPH2747_17.0.0.106(GLO)")!!.region)
    }

    @Test
    fun `blank version returns null`() {
        assertNull(OsBrand.parse(null))
        assertNull(OsBrand.parse(""))
        assertNull(OsBrand.parse("   "))
    }

    @Test
    fun `unknown variant code has null region`() {
        assertNull(OsBrand.parse("CPH2653_16.0.0.860(XX99)")!!.region)
    }
}
