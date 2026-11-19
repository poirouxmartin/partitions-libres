package com.musique.partitions.catalog

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class CatalogLanguageTest {
    private val french = listOf(
        "Domaine public", "domaine public", "Exclus", "par pièce", "Communauté",
        "Voix, piano", "Quatuor à cordes", "édition ancienne",
    )

    private fun appFile(rel: String): File =
        File("../app/$rel").takeIf { it.exists() } ?: File("app/$rel")

    @Test
    fun catalogJsonHasNoFrenchLiterals() {
        val text = appFile("src/main/assets/catalog.json").readText()
        french.forEach { assertTrue(it !in text, "catalog.json contains \"$it\"") }
    }

    @Test
    fun appSourcesHaveNoFrenchLiterals() {
        val root = appFile("src/main/kotlin")
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(files.isNotEmpty())
        files.forEach { f ->
            val text = f.readText()
            french.forEach { assertTrue(it !in text, "${f.name} contains \"$it\"") }
        }
    }
}
