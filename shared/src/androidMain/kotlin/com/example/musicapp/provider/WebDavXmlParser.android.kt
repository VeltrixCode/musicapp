package com.example.musicapp.provider

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

actual object WebDavXmlParser {
    actual fun parse(xml: String): List<WebDavItem> {
        if (xml.isBlank()) return emptyList()

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            isExpandEntityReferences = false
            // Explicitly disable external entities (XXE protection)
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            runCatching { setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true) }
        }

        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }

        val doc = try {
            builder.parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            return emptyList()
        }

        val results = mutableListOf<WebDavItem>()
        val root = doc.documentElement ?: return emptyList()

        fun getLocalTag(node: Node): String {
            return (node.localName ?: node.nodeName.substringAfter(':')).lowercase()
        }

        fun findDescendantText(element: Element, targetTag: String): String? {
            val children = element.childNodes
            for (i in 0 until children.length) {
                val child = children.item(i)
                if (child.nodeType == Node.ELEMENT_NODE) {
                    val childEl = child as Element
                    if (getLocalTag(childEl) == targetTag) {
                        return childEl.textContent?.trim()
                    }
                    val found = findDescendantText(childEl, targetTag)
                    if (found != null) return found
                }
            }
            return null
        }

        fun collectResponses(element: Element) {
            if (getLocalTag(element) == "response") {
                val href = findDescendantText(element, "href")
                if (!href.isNullOrBlank()) {
                    val displayName = findDescendantText(element, "displayname")
                    val contentType = findDescendantText(element, "getcontenttype")
                    val contentLength = findDescendantText(element, "getcontentlength")?.toLongOrNull()
                    results.add(WebDavItem(href, displayName, contentType, contentLength))
                }
            } else {
                val children = element.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        collectResponses(child as Element)
                    }
                }
            }
        }

        collectResponses(root)
        return results
    }
}
