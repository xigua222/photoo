package com.voicelike.app.motionphoto

import android.os.Build
import com.voicelike.app.MediaItem
import java.util.Locale

internal enum class DynamicPhotoExportContainer {
    SingleFileMotionPhoto,
    SamsungMotionPhoto,
    OplusMotionPhoto,
    XiaomiMotionPhoto,
    VendorPair
}

internal enum class VendorPairExportShell {
    Vivo,
    HuaweiHarmony,
    GenericPaired
}

internal fun resolveDynamicPhotoExportContainer(
    deviceBrand: String?,
    deviceManufacturer: String?,
    deviceModel: String?,
    sourceIsLivePhoto: Boolean,
    sourceIsMotionPhoto: Boolean,
    deviceProduct: String? = null,
    deviceDevice: String? = null,
    deviceDisplay: String? = null
): DynamicPhotoExportContainer {
    val deviceFingerprint = buildDeviceFingerprint(
        brand = deviceBrand,
        manufacturer = deviceManufacturer,
        model = deviceModel,
        product = deviceProduct,
        device = deviceDevice,
        display = deviceDisplay
    )

    return when {
        deviceFingerprint.contains("oppo") ||
            deviceFingerprint.contains("oplus") ||
            deviceFingerprint.contains("oneplus") ||
            deviceFingerprint.contains("realme") ||
            deviceFingerprint.contains("coloros") ->
            DynamicPhotoExportContainer.OplusMotionPhoto
        deviceFingerprint.contains("xiaomi") ||
            deviceFingerprint.contains("redmi") ||
            deviceFingerprint.contains("poco") ||
            deviceFingerprint.contains("miui") ||
            deviceFingerprint.contains("hyperos") ->
            DynamicPhotoExportContainer.XiaomiMotionPhoto
        deviceFingerprint.contains("samsung") ||
            deviceFingerprint.contains("galaxy") ||
            deviceFingerprint.contains("one ui") ->
            DynamicPhotoExportContainer.SamsungMotionPhoto
        deviceFingerprint.contains("vivo") ||
            deviceFingerprint.contains("huawei") ||
            deviceFingerprint.contains("honor") ->
            DynamicPhotoExportContainer.VendorPair
        sourceIsLivePhoto ->
            DynamicPhotoExportContainer.VendorPair
        sourceIsMotionPhoto ->
            DynamicPhotoExportContainer.SingleFileMotionPhoto
        else ->
            DynamicPhotoExportContainer.SingleFileMotionPhoto
    }
}

internal fun resolveDynamicPhotoExportContainerForCurrentDevice(
    sourceItem: MediaItem
): DynamicPhotoExportContainer {
    return resolveDynamicPhotoExportContainer(
        deviceBrand = Build.BRAND,
        deviceManufacturer = Build.MANUFACTURER,
        deviceModel = Build.MODEL,
        deviceProduct = Build.PRODUCT,
        deviceDevice = Build.DEVICE,
        deviceDisplay = Build.DISPLAY,
        sourceIsLivePhoto = sourceItem.isLivePhoto,
        sourceIsMotionPhoto = sourceItem.isMotionPhoto
    )
}

internal fun resolveVendorPairExportShell(
    deviceBrand: String?,
    deviceManufacturer: String?,
    deviceModel: String?
): VendorPairExportShell {
    val fingerprint = listOf(deviceBrand, deviceManufacturer, deviceModel)
        .joinToString(" ") { it.orEmpty() }
        .lowercase(Locale.ROOT)
    return when {
        fingerprint.contains("vivo") -> VendorPairExportShell.Vivo
        fingerprint.contains("huawei") || fingerprint.contains("honor") ->
            VendorPairExportShell.HuaweiHarmony
        else -> VendorPairExportShell.GenericPaired
    }
}

internal fun resolveVendorPairExportShellForCurrentDevice(): VendorPairExportShell {
    return resolveVendorPairExportShell(
        deviceBrand = Build.BRAND,
        deviceManufacturer = Build.MANUFACTURER,
        deviceModel = Build.MODEL
    )
}

private fun buildDeviceFingerprint(
    brand: String?,
    manufacturer: String?,
    model: String?,
    product: String?,
    device: String?,
    display: String?
): String {
    return listOf(brand, manufacturer, model, product, device, display)
        .map { it.orEmpty().trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
