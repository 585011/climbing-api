package com.example.climbingapi.integration

import com.example.climbingapi.config.R2Properties
import com.example.climbingapi.service.R2StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Live round-trip against a real Cloudflare R2 bucket — the one thing the mocked ITs cannot cover.
 * Skipped unless R2_ENDPOINT is set, so it never runs in CI or a normal `./gradlew build`.
 *
 * Run it manually with real credentials exported in your shell:
 *   export R2_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
 *   export R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... R2_BUCKET=climbing-wall-images
 *   ./gradlew test --tests "com.example.climbingapi.integration.R2StorageSmokeIT"
 */
@EnabledIfEnvironmentVariable(named = "R2_ENDPOINT", matches = ".+")
class R2StorageSmokeIT {

    private val props = R2Properties(
        endpoint = env("R2_ENDPOINT"),
        accessKeyId = env("R2_ACCESS_KEY_ID"),
        secretAccessKey = env("R2_SECRET_ACCESS_KEY"),
        bucket = env("R2_BUCKET"),
        presignDurationMinutes = 5
    )

    private val credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKeyId, props.secretAccessKey))
    private val serviceConfig = S3Configuration.builder().pathStyleAccessEnabled(true).build()

    private val s3 = S3Client.builder()
        .endpointOverride(URI.create(props.endpoint))
        .region(Region.of("auto"))
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfig)
        .build()

    private val presigner = S3Presigner.builder()
        .endpointOverride(URI.create(props.endpoint))
        .region(Region.of("auto"))
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfig)
        .build()

    private val storage = R2StorageService(s3, presigner, props)

    @Test
    fun `upload, presign-GET, then delete round-trips against R2`() {
        // 1x1 transparent PNG
        val png = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(),
            0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, 0x9C.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(),
            0x42, 0x60, 0x82.toByte()
        )

        val key = storage.upload(png, "image/png")
        println("Uploaded R2 object key: $key")
        assertTrue(key.startsWith("walls/") && key.endsWith(".png"), "unexpected key: $key")

        val url = storage.presignGet(key)
        // Don't log the full URL — it embeds the access key id and signature.
        println("Presigned GET host/path: ${URI.create(url).let { "${it.scheme}://${it.host}${it.path}" }}")
        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray()
        )

        assertEquals(200, response.statusCode(), "presigned GET should return 200")
        assertEquals(png.size, response.body().size, "downloaded bytes should match uploaded image")

        storage.delete(key)
        val afterDelete = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertTrue(afterDelete.statusCode() in setOf(403, 404), "object should be gone after delete, was ${afterDelete.statusCode()}")
        println("R2 round-trip OK: upload -> presign GET (200) -> delete -> gone (${afterDelete.statusCode()})")
    }

    private fun env(name: String): String =
        System.getenv(name) ?: error("$name must be set to run R2StorageSmokeIT")
}
