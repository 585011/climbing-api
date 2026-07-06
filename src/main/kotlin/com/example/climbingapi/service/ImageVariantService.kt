package com.example.climbingapi.service

import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
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

        if (exceedsPixelLimit(source.width, source.height)) {
            throw IllegalArgumentException("Image dimensions too large to process.")
        }

        return ImageVariants(
            optimized = resizeToJpeg(source, OPTIMIZED_MAX_WIDTH, OPTIMIZED_QUALITY),
            thumbnail = resizeToJpeg(source, THUMBNAIL_MAX_WIDTH, THUMBNAIL_QUALITY),
            contentType = "image/jpeg"
        )
    }

    private fun resizeToJpeg(source: BufferedImage, maxWidth: Int, quality: Double): ByteArray {
        val targetWidth = minOf(maxWidth, source.width) // never upscale
        val out = ByteArrayOutputStream()
        Thumbnails.of(source)
            .width(targetWidth) // height is computed to preserve aspect ratio
            .outputFormat("jpg")
            .outputQuality(quality)
            .toOutputStream(out)
        return out.toByteArray()
    }

    /** True when width × height exceeds the safe decode limit (guards against decompression bombs). */
    fun exceedsPixelLimit(width: Int, height: Int): Boolean =
        width.toLong() * height.toLong() > MAX_SOURCE_PIXELS

    companion object {
        const val THUMBNAIL_MAX_WIDTH = 400
        const val OPTIMIZED_MAX_WIDTH = 1080
        const val THUMBNAIL_QUALITY = 0.80
        const val OPTIMIZED_QUALITY = 0.82
        const val MAX_SOURCE_PIXELS = 50_000_000L // ~50 MP: generous for phone photos, blocks decode bombs
    }
}
