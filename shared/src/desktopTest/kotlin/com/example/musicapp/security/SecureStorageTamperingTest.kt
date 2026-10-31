package com.example.musicapp.security

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

class SecureStorageTamperingTest {

    @Test
    fun testSecureStorageReadWrite() {
        SecureStorage.saveSecret("test_api_key", "secret_value_12345")
        val retrieved = SecureStorage.getSecret("test_api_key")
        assertEquals("secret_value_12345", retrieved)
        SecureStorage.removeSecret("test_api_key")
    }

    @Test
    fun testDataTamperingIsRejectedByAuthenticationTagOrDpapi() {
        val originalText = "TopSecretAccountCredentials"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        // Encrypt with Desktop SecureStorage (DPAPI on Windows, AES-GCM on POSIX)
        val encrypted = SecureStorage.encryptBytes(originalBytes)
        assertNotNull(encrypted)

        // Corrupt a byte in the middle of ciphertext
        val tampered = encrypted.copyOf()
        val corruptIndex = tampered.size / 2
        tampered[corruptIndex] = (tampered[corruptIndex].toInt() xor 0xFF).toByte()

        // Decryption of tampered ciphertext MUST fail (cryptUnprotectData or GCM AEADBadTagException)
        assertFails {
            SecureStorage.decryptBytes(tampered)
        }
    }
}
