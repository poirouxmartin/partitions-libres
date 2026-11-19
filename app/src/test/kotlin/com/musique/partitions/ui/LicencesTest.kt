package com.musique.partitions.ui

import com.musique.partitions.catalog.CatalogParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LicencesTest {
    @Test
    fun everyBundledLicenceCodeHasALabel() {
        val catalog = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val codes = catalog.pieces.map { it.licence }.toSet()
        assertTrue(codes.isNotEmpty())
        codes.forEach { c ->
            assertNotNull(Licences.labelRes(c) ?: Licences.ccLabel(c), "no label for $c")
        }
    }

    @Test
    fun ccLabelFormatsVersions() {
        assertEquals("CC BY-SA 4.0", Licences.ccLabel("CC-BY-SA-4.0"))
        assertEquals("CC BY 3.0", Licences.ccLabel("CC-BY-3.0"))
        assertNull(Licences.ccLabel("PD"))
    }

    @Test
    fun unknownCodeHasNoResource() {
        assertNull(Licences.labelRes("XYZ"))
    }
}
