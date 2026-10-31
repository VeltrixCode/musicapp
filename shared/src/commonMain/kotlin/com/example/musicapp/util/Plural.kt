package com.example.musicapp.util

/**
 * Returns the proper Russian plural form for number [n].
 * E.g.: pluralRu(1, "трек", "трека", "треков") -> "1 трек"
 *       pluralRu(2, "трек", "трека", "треков") -> "2 трека"
 *       pluralRu(5, "трек", "трека", "треков") -> "5 треков"
 *       pluralRu(11, "трек", "трека", "треков") -> "11 треков"
 */
fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val absN = kotlin.math.abs(n)
    val rem100 = absN % 100
    val rem10 = absN % 10

    val word = when {
        rem100 in 11..19 -> many
        rem10 == 1 -> one
        rem10 in 2..4 -> few
        else -> many
    }
    return "$n $word"
}
