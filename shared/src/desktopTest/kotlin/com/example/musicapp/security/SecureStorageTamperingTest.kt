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

    @Test
    fun testAes256GcmFallbackEncryptionAndTamperingRejection() {
        val originalText = "SensitiveUserData_AES_GCM_Test"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        // Specifically test the AES-256-GCM fallback implementation
        val encrypted = SecureStorage.fallbackEncrypt(originalBytes)
        assertNotNull(encrypted)

        val decrypted = SecureStorage.fallbackDecrypt(encrypted)
        assertEquals(originalText, String(decrypted, StandardCharsets.UTF_8))

        // Corrupt a byte in the GCM ciphertext / authentication tag
        val tampered = encrypted.copyOf()
        val corruptIndex = tampered.size - 2 // Corrupt authentication tag area
        tampered[corruptIndex] = (tampered[corruptIndex].toInt() xor 0x55).toByte()

        assertFails {
            SecureStorage.fallbackDecrypt(tampered)
        }
    }

    @Test
    fun testDpapiOptionalEntropyBinding() {
        if (!com.sun.jna.Platform.isWindows()) return

        val originalText = "TokenProtectedWithCustomEntropy"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val entropy1 = "AppEntropy-Scope-A".toByteArray(StandardCharsets.UTF_8)
        val entropy2 = "AppEntropy-Scope-B".toByteArray(StandardCharsets.UTF_8)

        val encrypted = SecureStorage.encryptBytes(originalBytes, entropy1)
        assertNotNull(encrypted)

        // Decryption with the matching entropy succeeds
        val decrypted = SecureStorage.decryptBytes(encrypted, entropy1)
        assertEquals(originalText, String(decrypted, StandardCharsets.UTF_8))

        // Decryption with different entropy or null entropy MUST fail
        assertFails {
            SecureStorage.decryptBytes(encrypted, entropy2)
        }
        assertFails {
            SecureStorage.decryptBytes(encrypted, null)
        }
    }
}
