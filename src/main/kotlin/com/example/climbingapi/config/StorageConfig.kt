package com.example.climbingapi.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
@EnableConfigurationProperties(R2Properties::class)
class StorageConfig(private val props: R2Properties) {

    // R2 is S3-compatible: override the endpoint, use region "auto", and path-style access.
    private fun credentials() =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKeyId, props.secretAccessKey))

    private val serviceConfiguration: S3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build()

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(props.endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(credentials())
            .serviceConfiguration(serviceConfiguration)
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(props.endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(credentials())
            .serviceConfiguration(serviceConfiguration)
            .build()
}
