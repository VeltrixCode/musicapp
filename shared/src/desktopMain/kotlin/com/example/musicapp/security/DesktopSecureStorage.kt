package com.example.musicapp.security

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object SecureStorage {
    val vaultDir: File by lazy {
        File(System.getProperty("user.home"), "JVXMusic/.security").apply {
            mkdirs()
            setReadable(true, true)
            setWritable(true, true)
            setExecutable(true, true)
        }
    }

    /**
     * Where the encrypted vault file is stored:
     * - Windows: %USERPROFILE%\JVXMusic\.security\vault.dpapi
     * - macOS / Linux: ~/.config/JVXMusic/.security/vault.enc
     *
     * Security model:
     * - Windows: Uses the Windows Data Protection API (CryptProtectData / CryptUnprotectData via JNA).
     *   The encryption master key is tied directly to the current Windows user account login credentials
     *   managed by the Local Security Authority (LSA). It is NOT tied to a hardware TPM chip.
     *   Optional caller-supplied entropy can be provided to bind ciphertext to application-specific scope.
     * - macOS / Linux: Uses AES-256-GCM authenticated encryption (AES/GCM/NoPadding, 128-bit authentication
     *   tag, 12-byte random IV) with key derivation via PBKDF2WithHmacSHA256 (65,536 iterations) seeded
     *   with user entropy (user.name, user.home) and a cryptographically secure 16-byte persistent salt.
     */
    val vaultFile: File get() = File(vaultDir, if (Platform.isWindows()) "vault.dpapi" else "vault.enc")
    private val saltFile: File get() = File(vaultDir, "vault.salt")

    private val cachedProperties = Properties()
    private var isLoaded = false
    private val lock = Any()

    val accessCount = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Encrypts plaintext bytes. On Windows, uses CryptProtectData with optional entropy.
     * On macOS/Linux, uses AES-256-GCM.
     */
    fun encryptBytes(data: ByteArray, entropy: ByteArray? = null): ByteArray {
        return if (Platform.isWindows()) {
            if (entropy != null) {
                Crypt32Util.cryptProtectData(data, entropy, 0, null, null)
            } else {
                Crypt32Util.cryptProtectData(data)
            }
        } else {
            fallbackEncrypt(data)
        }
    }

    /**
     * Decrypts ciphertext bytes. On Windows, uses CryptUnprotectData with optional entropy.
     * On macOS/Linux, uses AES-256-GCM.
     */
    fun decryptBytes(encrypted: ByteArray, entropy: ByteArray? = null): ByteArray {
        return if (Platform.isWindows()) {
            if (entropy != null) {
                Crypt32Util.cryptUnprotectData(encrypted, entropy, 0, null)
            } else {
                Crypt32Util.cryptUnprotectData(encrypted)
            }
        } else {
            fallbackDecrypt(encrypted)
        }
    }

    private fun loadVault() {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            cachedProperties.clear()
            if (vaultFile.exists() && vaultFile.length() > 0) {
                try {
                    val raw = vaultFile.readBytes()
                    val decrypted = decryptBytes(raw)
                    val text = String(decrypted, StandardCharsets.UTF_8)
                    cachedProperties.load(StringReader(text))
                } catch (_: Exception) {
                    cachedProperties.clear()
                }
            }
            isLoaded = true
        }
    }

    private fun persistVault() {
        synchronized(lock) {
            try {
                val sw = StringWriter()
                cachedProperties.store(sw, "JVXMusic System Vault")
                val plainBytes = sw.toString().toByteArray(StandardCharsets.UTF_8)
                val encrypted = encryptBytes(plainBytes)

                val tmp = File(vaultDir, "vault.tmp")
                tmp.writeBytes(encrypted)
                tmp.setReadable(true, true)
                tmp.setWritable(true, true)

                tmp.renameTo(vaultFile)
                vaultFile.setReadable(true, true)
                vaultFile.setWritable(true, true)
            } catch (_: Exception) {}
        }
    }

    actual fun saveSecret(key: String, value: String) {
        accessCount.incrementAndGet()
        loadVault()
        synchronized(lock) {
            cachedProperties.setProperty(key, value)
            persistVault()
        }
    }

    actual fun getSecret(key: String): String? {
        accessCount.incrementAndGet()
        loadVault()
        synchronized(lock) {
            return cachedProperties.getProperty(key)
        }
    }

    actual fun removeSecret(key: String) {
        accessCount.incrementAndGet()
        loadVault()
        synchronized(lock) {
            if (cachedProperties.containsKey(key)) {
                cachedProperties.remove(key)
                persistVault()
            }
        }
    }

    // --- POSIX Fallback (macOS / Linux AES-256-GCM) ---
    private fun getOrCreateSalt(): ByteArray {
        if (!saltFile.exists() || saltFile.length() < 16) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            saltFile.writeBytes(salt)
            return salt
        }
        return saltFile.readBytes()
    }

    private fun deriveKey(): SecretKeySpec {
        val salt = getOrCreateSalt()
        val userEntropy = "${System.getProperty("user.name")}:${System.getProperty("user.home")}:JVXMusicPOSIXKey"
        val spec = PBEKeySpec(userEntropy.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    internal fun fallbackEncrypt(data: ByteArray): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(data)
        val output = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(cipherText, 0, output, iv.size, cipherText.size)
        return output
    }

    internal fun fallbackDecrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val cipherText = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }
}
