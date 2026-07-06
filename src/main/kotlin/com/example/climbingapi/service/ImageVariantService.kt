package com.example.climbingapi.service

import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

data class ImageVariants(
    val optimized: ByteArray,
    val thumbnail: ByteArray,
    val contentType: String
)

/**
 * Produces resized JPEG variants of an uploaded image: a small thumbnail for
 * list views and an optimized full-size for detail views. The caller keeps the
 * original separately.
 */
@Service
class ImageVariantService {

    fun generate(bytes: ByteArray, contentType: String): ImageVariants {
        val source = try {
            ImageIO.read(ByteArrayInputStream(bytes))
        } catch (_: Exception) {
            null
        }

        // ImageIO cannot decode WebP (and returns null for any unreadable input).
        // WebP uploads are already small, so fall back to the original bytes for
        // both variants rather than pulling in a native decoder.
        if (source == null) {
            if (contentType == "image/webp") {
                return ImageVariants(optimized = bytes, thumbnail = bytes, contentType = contentType)
            }
            throw IllegalArgumentException("Could not decode image for processing.")
        }

        return ImageVariants(
            optimized = resizeToJpeg(bytes, OPTIMIZED_MAX_WIDTH, OPTIMIZED_QUALITY, source.width),
            thumbnail = resizeToJpeg(bytes, THUMBNAIL_MAX_WIDTH, THUMBNAIL_QUALITY, source.width),
            contentType = "image/jpeg"
        )
    }

    private fun resizeToJpeg(bytes: ByteArray, maxWidth: Int, quality: Double, sourceWidth: Int): ByteArray {
        val targetWidth = minOf(maxWidth, sourceWidth) // never upscale
        val out = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(bytes))
            .width(targetWidth) // height is computed to preserve aspect ratio
            .outputFormat("jpg")
            .outputQuality(quality)
            .toOutputStream(out)
        return out.toByteArray()
    }

    companion object {
        const val THUMBNAIL_MAX_WIDTH = 400
        const val OPTIMIZED_MAX_WIDTH = 1080
        const val THUMBNAIL_QUALITY = 0.80
        const val OPTIMIZED_QUALITY = 0.82
    }
}
