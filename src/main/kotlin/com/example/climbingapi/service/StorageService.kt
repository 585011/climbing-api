package com.example.climbingapi.service

/**
 * Object storage for wall images (Cloudflare R2 in production).
 * Implementations persist raw bytes and return an opaque object key; the key is what
 * is stored on the wall record. Reads turn the key back into a short-lived presigned URL.
 */
interface StorageService {

    /** Uploads the given bytes and returns the generated object key. */
    fun upload(bytes: ByteArray, contentType: String): String

    /** Best-effort delete; failures are logged, not thrown. */
    fun delete(key: String)

    /** Returns a short-lived presigned GET URL for the object key. */
    fun presignGet(key: String): String
}
