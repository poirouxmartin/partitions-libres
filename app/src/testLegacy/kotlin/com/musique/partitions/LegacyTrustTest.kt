package com.musique.partitions

import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Android 7.0 lacks the Let's Encrypt roots, so HTTPS to Mutopia, raw.githubusercontent.com and the
 * GitHub release downloads failed there and the viewer said "No connection". The legacy flavor must
 * trust the bundled ISRG roots on top of the system store.
 */
class LegacyTrustTest {
    private val legacy = File("src/legacy")

    private fun sha256(c: X509Certificate) =
        MessageDigest.getInstance("SHA-256").digest(c.encoded).joinToString("") { "%02X".format(it) }

    @Test
    fun manifestUsesTheNetworkSecurityConfig() {
        val manifest = File(legacy, "AndroidManifest.xml")
        assertTrue(manifest.isFile, "legacy manifest overlay missing")
        assertTrue("android:networkSecurityConfig=\"@xml/network_security_config\"" in manifest.readText())
    }

    @Test
    fun configTrustsSystemAndEveryBundledRoot() {
        val config = File(legacy, "res/xml/network_security_config.xml")
        assertTrue(config.isFile, "network_security_config.xml missing")
        val xml = config.readText()
        assertTrue("src=\"system\"" in xml, "system roots must stay trusted")
        val raws = File(legacy, "res/raw").listFiles().orEmpty().filter { it.extension == "pem" }
        assertTrue(raws.isNotEmpty())
        raws.forEach { assertTrue("src=\"@raw/${it.nameWithoutExtension}\"" in xml, "${it.name} not referenced") }
    }

    @Test
    fun bundlesTheRealIsrgRootX1() {
        val pem = File(legacy, "res/raw/isrg_root_x1.pem")
        assertTrue(pem.isFile, "ISRG Root X1 not bundled")
        val cert = pem.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) } as X509Certificate
        assertTrue(sha256(cert) == "96BCEC06264976F37460779ACF28C5A7CFE8A3C0AAE11A8FFCEE05C0BDDF08C6")
    }
}
