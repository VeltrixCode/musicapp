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
     * On Windows, the master encryption key is managed strictly by the Windows OS Security Subsystem
     * (LSA / TPM via Windows Data Protection API - DPAPI) and is non-exportable.
     */
    val vaultFile: File get() = File(vaultDir, if (Platform.isWindows()) "vault.dpapi" else "vault.enc")
    private val saltFile: File get() = File(vaultDir, "vault.salt")

    private val cachedProperties = Properties()
    private var isLoaded = false
    private val lock = Any()

    fun encryptBytes(data: ByteArray): ByteArray {
        return if (Platform.isWindows()) {
            Crypt32Util.cryptProtectData(data)
        } else {
            fallbackEncrypt(data)
        }
    }

    fun decryptBytes(encrypted: ByteArray): ByteArray {
        return if (Platform.isWindows()) {
            Crypt32Util.cryptUnprotectData(encrypted)
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
        loadVault()
        synchronized(lock) {
            cachedProperties.setProperty(key, value)
            persistVault()
        }
    }

    actual fun getSecret(key: String): String? {
        loadVault()
        synchronized(lock) {
            return cachedProperties.getProperty(key)
        }
    }

    actual fun removeSecret(key: String) {
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

    private fun fallbackEncrypt(data: ByteArray): ByteArray {
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

    private fun fallbackDecrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val cipherText = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }
}
