package com.example.climbingapi.service

import com.example.climbingapi.config.R2Properties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
class R2StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val props: R2Properties
) : StorageService {

    override fun upload(bytes: ByteArray, contentType: String): String {
        val key = "walls/${UUID.randomUUID()}.${extensionFor(contentType)}"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        )
        return key
    }

    override fun delete(key: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(props.bucket).key(key).build()
            )
        } catch (e: SdkException) {
            logger.warn("Failed to delete R2 object {}: {}", key, e.message)
        }
    }

    override fun presignGet(key: String): String {
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(props.presignDurationMinutes))
            .getObjectRequest(GetObjectRequest.builder().bucket(props.bucket).key(key).build())
            .build()
        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    private fun extensionFor(contentType: String): String = when (contentType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> throw IllegalArgumentException("Unsupported image type: $contentType")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(R2StorageService::class.java)
    }
}