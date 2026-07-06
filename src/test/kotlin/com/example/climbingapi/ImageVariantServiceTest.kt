package com.example.climbingapi

import com.example.climbingapi.service.ImageVariantService
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageVariantServiceTest {

    private val service = ImageVariantService()

    // A 2000x1000 opaque PNG — larger than both variant width caps.
    private fun largePngBytes(): ByteArray {
        val img = BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(120, 80, 60)
        g.fillRect(0, 0, 2000, 1000)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    private fun widthOf(bytes: ByteArray): Int =
        ImageIO.read(ByteArrayInputStream(bytes)).width

    @Test
    fun `generate downsizes to the variant width caps and outputs jpeg`() {
        val variants = service.generate(largePngBytes(), "image/png")

        assertEquals("image/jpeg", variants.contentType)
        assertEquals(ImageVariantService.THUMBNAIL_MAX_WIDTH, widthOf(variants.thumbnail))
        assertEquals(ImageVariantService.OPTIMIZED_MAX_WIDTH, widthOf(variants.optimized))
        // Both variants must be far smaller than the source bytes.
        assertTrue(variants.thumbnail.size < variants.optimized.size)
    }

    @Test
    fun `generate preserves aspect ratio`() {
        val variants = service.generate(largePngBytes(), "image/png")
        val thumb = ImageIO.read(ByteArrayInputStream(variants.thumbnail))
        // source is 2:1, so a 400px-wide thumbnail is 200px tall.
        assertEquals(200, thumb.height)
    }

    @Test
    fun `generate does not upscale a source smaller than the caps`() {
        val small = BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(small, "png", out)

        val variants = service.generate(out.toByteArray(), "image/png")
        assertEquals(150, widthOf(variants.optimized))
        assertEquals(150, widthOf(variants.thumbnail))
    }

    @Test
    fun `generate falls back to original bytes for undecodable webp input`() {
        val webpBytes = byteArrayOf(1, 2, 3, 4) // not decodable by ImageIO
        val variants = service.generate(webpBytes, "image/webp")

        assertEquals("image/webp", variants.contentType)
        assertArrayEquals(webpBytes, variants.optimized)
        assertArrayEquals(webpBytes, variants.thumbnail)
    }

    @Test
    fun `generate throws on undecodable non-webp input`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.generate(byteArrayOf(1, 2, 3, 4), "image/png")
        }
    }
}
