package com.example.musicapp.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object SecureStorage {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "JVXMusicMasterKey"

    private var appContext: Context? = null
    private val cachedProperties = Properties()
    private var isLoaded = false
    private val lock = Any()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun encryptBytes(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(data)
        val output = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(cipherText, 0, output, iv.size, cipherText.size)
        return output
    }

    fun decryptBytes(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, 12)
        val cipherText = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        return cipher.doFinal(cipherText)
    }

    private fun getVaultFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, "vault.keystore.enc")
    }

    private fun loadVault() {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            val file = getVaultFile()
            if (file != null && file.exists() && file.length() > 28) {
                try {
                    val raw = file.readBytes()
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
        val file = getVaultFile() ?: return
        synchronized(lock) {
            try {
                val sw = StringWriter()
                cachedProperties.store(sw, "JVXMusic Android Keystore Vault")
                val plainBytes = sw.toString().toByteArray(StandardCharsets.UTF_8)
                val encrypted = encryptBytes(plainBytes)

                val tmp = File(file.parentFile, "vault.tmp")
                tmp.writeBytes(encrypted)
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
