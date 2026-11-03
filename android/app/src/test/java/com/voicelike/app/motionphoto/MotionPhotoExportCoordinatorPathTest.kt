package com.voicelike.app.motionphoto

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionPhotoExportCoordinatorPathTest {

    @Test
    fun `oplus motion photo uses camera roll relative path`() {
        assertEquals(
            "DCIM/Camera",
            MotionPhotoExportCoordinator.resolveMotionPhotoGalleryRelativePath(
                DynamicPhotoExportContainer.OplusMotionPhoto
            )
        )
    }

    @Test
    fun `generic motion photo keeps photoo folder relative path`() {
        assertEquals(
            "Pictures/Photoo/MotionPhoto",
            MotionPhotoExportCoordinator.resolveMotionPhotoGalleryRelativePath(
                DynamicPhotoExportContainer.SingleFileMotionPhoto
            )
        )
    }
}
