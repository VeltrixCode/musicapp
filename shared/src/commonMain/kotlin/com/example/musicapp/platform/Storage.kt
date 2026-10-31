package com.example.musicapp.platform

/** Small key-value file storage in the app's private data directory. */
expect object AppStorage {
    fun read(name: String): String?
    fun write(name: String, text: String)
    fun writeAtomic(name: String, text: String)
    fun readBytes(name: String): ByteArray?
    fun writeBytes(name: String, bytes: ByteArray)
    /** Deletes a file by absolute path, such as an uploaded song. Missing files are ignored. */
    fun deleteFile(absolutePath: String)
    /** Returns the absolute path of the local audio cache directory. */
    fun getCacheDir(): String
}
