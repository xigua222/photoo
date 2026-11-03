package com.voicelike.app.motionphoto

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

internal object MotionPhotoContainerWriter {

    fun writeMotionPhoto(
        jpegFile: File,
        videoFile: File,
        outputFile: File
    ): File {
        require(jpegFile.exists()) { "Missing jpeg file" }
        require(videoFile.exists()) { "Missing video file" }

        val videoLength = videoFile.length().coerceAtLeast(1L)
        val offsetFromEnd = videoLength
        writeXmp(
            jpegFile = jpegFile,
            offsetFromEnd = offsetFromEnd,
            videoLength = videoLength
        )

        FileOutputStream(outputFile).use { output ->
            jpegFile.inputStream().use { input -> input.copyTo(output) }
            videoFile.inputStream().use { input -> input.copyTo(output) }
        }
        return outputFile
    }

    private fun writeXmp(
        jpegFile: File,
        offsetFromEnd: Long,
        videoLength: Long
    ) {
        val exif = ExifInterface(jpegFile.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_XMP,
            buildXmp(
                offsetFromEnd = offsetFromEnd,
                videoLength = videoLength
            )
        )
        exif.saveAttributes()
    }

    private fun buildXmp(
        offsetFromEnd: Long,
        videoLength: Long
    ): String {
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description
                  xmlns:Camera="http://ns.google.com/photos/1.0/camera/"
                  xmlns:Container="http://ns.google.com/photos/1.0/container/"
                  xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                  xmlns:GContainer="http://ns.google.com/photos/1.0/container/"
                  xmlns:GContainerItem="http://ns.google.com/photos/1.0/container/item/"
                  Camera:MotionPhoto="1"
                  Camera:MotionPhotoVersion="1"
                  Camera:MotionPhotoPresentationTimestampUs="-1">
                  <Container:Directory>
                    <Container:Item
                      Item:Mime="image/jpeg"
                      Item:Semantic="Primary"
                      Item:Length="0"
                      Item:Padding="0" />
                    <Container:Item
                      Item:Mime="video/mp4"
                      Item:Semantic="MotionPhoto"
                      Item:Length="$videoLength"
                      Item:Padding="0" />
                  </Container:Directory>
                  <GContainer:Directory>
                    <GContainer:Item
                      GContainerItem:Mime="image/jpeg"
                      GContainerItem:Semantic="Primary"
                      GContainerItem:Length="0"
                      GContainerItem:Padding="0" />
                    <GContainer:Item
                      GContainerItem:Mime="video/mp4"
                      GContainerItem:Semantic="MotionPhoto"
                      GContainerItem:Length="$videoLength"
                      GContainerItem:Padding="0" />
                  </GContainer:Directory>
                </rdf:Description>
                <rdf:Description
                  xmlns:Camera="http://ns.google.com/photos/1.0/camera/"
                  Camera:MicroVideo="1"
                  Camera:MicroVideoVersion="1"
                  Camera:MicroVideoOffset="$offsetFromEnd" />
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }
}
