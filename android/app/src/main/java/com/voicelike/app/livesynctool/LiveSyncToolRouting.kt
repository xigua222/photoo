package com.voicelike.app.livesynctool

import android.os.Build
import java.util.Locale

internal object LiveSyncToolRouting {
    fun resolveTargetFamily(
        deviceBrand: String? = Build.BRAND,
        deviceManufacturer: String? = Build.MANUFACTURER,
        deviceModel: String? = Build.MODEL,
        deviceProduct: String? = Build.PRODUCT,
        deviceDevice: String? = Build.DEVICE,
        deviceDisplay: String? = Build.DISPLAY
    ): LiveSyncToolTargetFamily {
        val fingerprint = listOf(
            deviceBrand,
            deviceManufacturer,
            deviceModel,
            deviceProduct,
            deviceDevice,
            deviceDisplay
        )
            .map { it.orEmpty().trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return when {
            fingerprint.contains("oppo") ||
                fingerprint.contains("oplus") ||
                fingerprint.contains("oneplus") ||
                fingerprint.contains("realme") ||
                fingerprint.contains("coloros") ->
                LiveSyncToolTargetFamily.OplusMotionPhoto
            fingerprint.contains("xiaomi") ||
                fingerprint.contains("redmi") ||
                fingerprint.contains("poco") ||
                fingerprint.contains("miui") ||
                fingerprint.contains("hyperos") ->
                LiveSyncToolTargetFamily.XiaomiMotionPhoto
            fingerprint.contains("vivo") ||
                fingerprint.contains("huawei") ||
                fingerprint.contains("honor") ->
                LiveSyncToolTargetFamily.VendorPair
            else ->
                LiveSyncToolTargetFamily.SingleFileMotionPhoto
        }
    }

    fun targetLabel(targetFamily: LiveSyncToolTargetFamily): String {
        return when (targetFamily) {
            LiveSyncToolTargetFamily.SingleFileMotionPhoto -> "通用 Motion Photo"
            LiveSyncToolTargetFamily.XiaomiMotionPhoto -> "小米 Motion Photo"
            LiveSyncToolTargetFamily.VendorPair -> "本机 Live Photo"
            LiveSyncToolTargetFamily.OplusMotionPhoto -> "OPPO Motion Photo"
        }
    }

    fun resolveVendorPairShell(
        deviceBrand: String? = Build.BRAND,
        deviceManufacturer: String? = Build.MANUFACTURER,
        deviceModel: String? = Build.MODEL
    ): LiveSyncToolVendorPairShell {
        val fingerprint = listOf(deviceBrand, deviceManufacturer, deviceModel)
            .joinToString(" ") { it.orEmpty() }
            .lowercase(Locale.ROOT)
        return when {
            fingerprint.contains("vivo") -> LiveSyncToolVendorPairShell.Vivo
            fingerprint.contains("huawei") || fingerprint.contains("honor") ->
                LiveSyncToolVendorPairShell.HuaweiHarmony
            else -> LiveSyncToolVendorPairShell.GenericPaired
        }
    }
}

internal enum class LiveSyncToolVendorPairShell {
    Vivo,
    HuaweiHarmony,
    GenericPaired
}
