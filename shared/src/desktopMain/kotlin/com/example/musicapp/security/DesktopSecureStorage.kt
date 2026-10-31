package com.example.musicapp.security

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object SecureStorage {
    private val vaultDir: File by lazy {
        File(System.getProperty("user.home"), "JVXMusic/.security").apply {
            mkdirs()
            // Set directory readable and writable only by user
            setReadable(true, true)
            setWritable(true, true)
            setExecutable(true, true)
        }
    }

    private val vaultFile: File get() = File(vaultDir, "vault.enc")
    private val saltFile: File get() = File(vaultDir, "vault.salt")

    private val cachedProperties = Properties()
    private var isLoaded = false

    private val lock = Any()

    private fun getOrCreateSalt(): ByteArray {
        if (!saltFile.exists() || saltFile.length() < 16) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            saltFile.writeBytes(salt)
            saltFile.setReadable(true, true)
            saltFile.setWritable(true, true)
            return salt
        }
        return saltFile.readBytes()
    }

    private fun deriveKey(): SecretKeySpec {
        val salt = getOrCreateSalt()
        // Machine-bound user entropy
        val userEntropy = "${System.getProperty("user.name")}:${System.getProperty("user.home")}:JVXMusicSecureKey"
        val spec = PBEKeySpec(userEntropy.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun loadVault() {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            cachedProperties.clear()
            if (vaultFile.exists() && vaultFile.length() > 28) {
                try {
                    val raw = vaultFile.readBytes()
                    val iv = raw.copyOfRange(0, 12)
                    val cipherText = raw.copyOfRange(12, raw.size)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, deriveKey(), spec)
                    val decrypted = cipher.doFinal(cipherText)

                    val text = String(decrypted, StandardCharsets.UTF_8)
                    cachedProperties.load(text.reader())
                } catch (_: Exception) {
                    // Corrupted or modified key; start fresh
                    cachedProperties.clear()
                }
            }
            isLoaded = true
        }
    }

    private fun persistVault() {
        synchronized(lock) {
            try {
                val sw = java.io.StringWriter()
                cachedProperties.store(sw, "JVXMusic Encrypted Vault")
                val plainBytes = sw.toString().toByteArray(StandardCharsets.UTF_8)

                val iv = ByteArray(12)
                SecureRandom().nextBytes(iv)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), spec)
                val cipherText = cipher.doFinal(plainBytes)

                val output = ByteArray(iv.size + cipherText.size)
                System.arraycopy(iv, 0, output, 0, iv.size)
                System.arraycopy(cipherText, 0, output, iv.size, cipherText.size)

                val tmp = File(vaultDir, "vault.tmp")
                tmp.writeBytes(output)
                tmp.setReadable(true, true)
                tmp.setWritable(true, true)

                // Atomic rename
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
}
