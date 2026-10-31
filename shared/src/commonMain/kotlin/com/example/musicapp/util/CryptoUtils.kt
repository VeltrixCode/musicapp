package com.example.musicapp.util

/**
 * Pure Kotlin MD5 implementation for Subsonic token authentication and hashing
 * without external platform dependencies.
 */
object CryptoUtils {
    private val SHIFT = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    )

    private val TABLE_T = IntArray(64) { i ->
        ((1L shl 32) * kotlin.math.abs(kotlin.math.sin(i + 1.0))).toLong().toInt()
    }

    fun md5Hex(input: String): String {
        val bytes = input.encodeToByteArray()
        val bitLength = bytes.size.toLong() * 8L

        val paddingLength = if (bytes.size % 64 < 56) {
            56 - (bytes.size % 64)
        } else {
            64 + 56 - (bytes.size % 64)
        }

        val padded = ByteArray(bytes.size + paddingLength + 8)
        bytes.copyInto(padded)
        padded[bytes.size] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[bytes.size + paddingLength + i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
        }

        var a = 0x67452301
        var b = 0xefcdab89.toInt()
        var c = 0x98badcfe.toInt()
        var d = 0x10325476

        val x = IntArray(16)
        for (i in padded.indices step 64) {
            for (j in 0 until 16) {
                x[j] = (padded[i + j * 4].toInt() and 0xFF) or
                        ((padded[i + j * 4 + 1].toInt() and 0xFF) shl 8) or
                        ((padded[i + j * 4 + 2].toInt() and 0xFF) shl 16) or
                        ((padded[i + j * 4 + 3].toInt() and 0xFF) shl 24)
            }

            var aa = a
            var bb = b
            var cc = c
            var dd = d

            for (j in 0 until 64) {
                val f: Int
                val g: Int
                when (j) {
                    in 0..15 -> {
                        f = (bb and cc) or (bb.inv() and dd)
                        g = j
                    }
                    in 16..31 -> {
                        f = (dd and bb) or (dd.inv() and cc)
                        g = (5 * j + 1) % 16
                    }
                    in 32..47 -> {
                        f = bb xor cc xor dd
                        g = (3 * j + 5) % 16
                    }
                    else -> {
                        f = cc xor (bb or dd.inv())
                        g = (7 * j) % 16
                    }
                }

                val temp = dd
                dd = cc
                cc = bb
                bb += Integer.rotateLeft(aa + f + TABLE_T[j] + x[g], SHIFT[j])
                aa = temp
            }

            a += aa
            b += bb
            c += cc
            d += dd
        }

        fun toHex(value: Int): String {
            val sb = StringBuilder(8)
            for (i in 0 until 4) {
                val byte = (value ushr (i * 8)) and 0xFF
                val hex = byte.toString(16)
                if (hex.length == 1) sb.append('0')
                sb.append(hex)
            }
            return sb.toString()
        }

        return toHex(a) + toHex(b) + toHex(c) + toHex(d)
    }

    private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun base64Encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            val c0 = b0 ushr 2
            val c1 = ((b0 and 0x03) shl 4) or (if (b1 >= 0) (b1 ushr 4) else 0)
            val c2 = if (b1 >= 0) (((b1 and 0x0F) shl 2) or (if (b2 >= 0) (b2 ushr 6) else 0)) else -1
            val c3 = if (b2 >= 0) (b2 and 0x3F) else -1

            sb.append(BASE64_CHARS[c0])
            sb.append(BASE64_CHARS[c1])
            sb.append(if (c2 >= 0) BASE64_CHARS[c2] else '=')
            sb.append(if (c3 >= 0) BASE64_CHARS[c3] else '=')
        }
        return sb.toString()
    }
}
