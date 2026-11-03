package com.voicelike.app.motionphoto

import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicPhotoExportRoutingTest {

    @Test
    fun `xiaomi devices prefer single-file motion photo even for imported live photo`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "Redmi",
            deviceManufacturer = "Xiaomi",
            deviceModel = "23113RKC6C",
            sourceIsLivePhoto = true,
            sourceIsMotionPhoto = false
        )

        assertEquals(DynamicPhotoExportContainer.XiaomiMotionPhoto, result)
    }

    @Test
    fun `hyperos fingerprint still resolves to xiaomi container`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "unknown",
            deviceManufacturer = "unknown",
            deviceModel = "24117RK2CC",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true,
            deviceDisplay = "HyperOS 2.0.3.0"
        )

        assertEquals(DynamicPhotoExportContainer.XiaomiMotionPhoto, result)
    }

    @Test
    fun `oppo devices prefer oplus motion photo container even for imported live photo`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "OPPO",
            deviceManufacturer = "OPPO",
            deviceModel = "PKJ110",
            sourceIsLivePhoto = true,
            sourceIsMotionPhoto = false
        )

        assertEquals(DynamicPhotoExportContainer.OplusMotionPhoto, result)
    }

    @Test
    fun `oneplus devices share the oplus motion photo container`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "OnePlus",
            deviceManufacturer = "OnePlus",
            deviceModel = "PJZ110",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true
        )

        assertEquals(DynamicPhotoExportContainer.OplusMotionPhoto, result)
    }

    @Test
    fun `realme devices share the oplus motion photo container`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "realme",
            deviceManufacturer = "realme",
            deviceModel = "RMX3851",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true
        )

        assertEquals(DynamicPhotoExportContainer.OplusMotionPhoto, result)
    }

    @Test
    fun `samsung devices use dedicated samsung motion photo container`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "samsung",
            deviceManufacturer = "samsung",
            deviceModel = "SM-S9280",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true
        )

        assertEquals(DynamicPhotoExportContainer.SamsungMotionPhoto, result)
    }

    @Test
    fun `vivo devices prefer vendor pair even for imported motion photo`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "vivo",
            deviceManufacturer = "vivo",
            deviceModel = "V2337A",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true
        )

        assertEquals(DynamicPhotoExportContainer.VendorPair, result)
    }

    @Test
    fun `unknown devices fall back to source live photo pairing`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "unknown",
            deviceManufacturer = "unknown",
            deviceModel = "generic",
            sourceIsLivePhoto = true,
            sourceIsMotionPhoto = false
        )

        assertEquals(DynamicPhotoExportContainer.VendorPair, result)
    }

    @Test
    fun `unknown devices do not become xiaomi just because source is motion photo`() {
        val result = resolveDynamicPhotoExportContainer(
            deviceBrand = "unknown",
            deviceManufacturer = "unknown",
            deviceModel = "generic",
            sourceIsLivePhoto = false,
            sourceIsMotionPhoto = true
        )

        assertEquals(DynamicPhotoExportContainer.SingleFileMotionPhoto, result)
    }

    @Test
    fun `vendor pair shell follows current device family`() {
        assertEquals(
            VendorPairExportShell.Vivo,
            resolveVendorPairExportShell(
                deviceBrand = "vivo",
                deviceManufacturer = "vivo",
                deviceModel = "V2337A"
            )
        )
        assertEquals(
            VendorPairExportShell.HuaweiHarmony,
            resolveVendorPairExportShell(
                deviceBrand = "HONOR",
                deviceManufacturer = "HONOR",
                deviceModel = "PTH-AN00"
            )
        )
        assertEquals(
            VendorPairExportShell.GenericPaired,
            resolveVendorPairExportShell(
                deviceBrand = "unknown",
                deviceManufacturer = "unknown",
                deviceModel = "generic"
            )
        )
    }
}
