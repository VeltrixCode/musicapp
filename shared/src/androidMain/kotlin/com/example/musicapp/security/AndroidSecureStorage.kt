package com.example.musicapp.security

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object SecureStorage {
    private var appContext: Context? = null
    private val cachedProperties = Properties()
    private var isLoaded = false
    private val lock = Any()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getVaultFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, "vault.enc")
    }

    private fun getSaltFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, "vault.salt")
    }

    private fun getOrCreateSalt(): ByteArray {
        val saltFile = getSaltFile() ?: return ByteArray(16)
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
        val pkg = appContext?.packageName ?: "com.example.musicapp"
        val spec = PBEKeySpec(pkg.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun loadVault() {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            val file = getVaultFile()
            if (file != null && file.exists() && file.length() > 28) {
                try {
                    val raw = file.readBytes()
                    val iv = raw.copyOfRange(0, 12)
                    val cipherText = raw.copyOfRange(12, raw.size)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, deriveKey(), spec)
                    val decrypted = cipher.doFinal(cipherText)

                    val text = String(decrypted, StandardCharsets.UTF_8)
                    cachedProperties.load(text.reader())
                } catch (_: Exception) {
                    cachedProperties.clear()
                }
            }
            isLoaded = true
        }
    }

    private fun persistVault() {
        val file = getVaultFile() ?: return
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

                val tmp = File(file.parentFile, "vault.tmp")
                tmp.writeBytes(output)
                tmp.renameTo(file)
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
