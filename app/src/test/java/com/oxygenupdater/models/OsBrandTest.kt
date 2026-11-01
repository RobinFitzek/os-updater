package com.oxygenupdater.models

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class OsBrandTest {

    @Test
    fun `parses OxygenOS marketing version with EU variant`() {
        val info = parseOsBrand("OxygenOS 15.0.0.850(EX01)")!!
        assertEquals("OxygenOS", info.name)
        assertEquals(15, info.major)
        assertEquals("EX01", info.variantCode)
        assertEquals("EU", info.region)
        assertFalse(info.isColorOsEra)
        assertEquals("OxygenOS ", info.prefix)
    }

    @Test
    fun `parses ColorOS 17 marketing version with India variant`() {
        val info = parseOsBrand("ColorOS 17.0.0.102(IN01)")!!
        assertEquals("ColorOS", info.name)
        assertEquals(17, info.major)
        assertTrue(info.isColorOsEra)
        assertEquals("India", info.region)
    }

    @Test
    fun `parses OTA version without OS name using major after model prefix`() {
        val info = parseOsBrand("CPH2649_17.0.0.102(EX01)")!!
        assertEquals(17, info.major)
        assertEquals("EX01", info.variantCode)
        // No OxygenOS 17 exists: major >= 17 means unified ColorOS era
        assertTrue(info.isColorOsEra)
    }

    @Test
    fun `OxygenOS 16 OTA version is not ColorOS era`() {
        val info = parseOsBrand("CPH2653_16.0.0.860(CN01)")!!
        assertEquals(16, info.major)
        assertFalse(info.isColorOsEra)
        assertEquals("China", info.region)
    }

    @Test
    fun `global variant maps to Global`() {
        assertEquals("Global", parseOsBrand("CPH2747_17.0.0.106(GLO)")!!.region)
    }

    @Test
    fun `blank version returns null`() {
        assertNull(parseOsBrand(null))
        assertNull(parseOsBrand(""))
        assertNull(parseOsBrand("   "))
    }

    @Test
    fun `unknown variant code has null region`() {
        assertNull(parseOsBrand("CPH2653_16.0.0.860(XX99)")!!.region)
    }
}
