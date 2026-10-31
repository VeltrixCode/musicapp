package com.example.musicapp.provider

data class WebDavItem(
    val href: String,
    val displayName: String? = null,
    val contentType: String? = null,
    val contentLength: Long? = null,
)

expect object WebDavXmlParser {
    fun parse(xml: String): List<WebDavItem>
}
