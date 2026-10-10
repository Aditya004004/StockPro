package com.company.stockpro.core.data.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * High-performance cryptographic checksum utility.
 * Calculates tamper-evident SHA-256 hashes of generated export files.
 */
object ChecksumUtil {

    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        buildString {
            for (b in hashBytes) {
                append(String.format("%02x", b))
            }
        }
    }
}
