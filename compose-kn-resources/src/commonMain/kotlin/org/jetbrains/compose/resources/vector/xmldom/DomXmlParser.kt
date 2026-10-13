package org.jetbrains.compose.resources.vector.xmldom

/**
 * Pure-Kotlin XML → [Element]（供 Android VectorDrawable 解析；无 JVM DOM / NSXMLParser）。
 *
 * 足以覆盖 `vector` / `path` / `group` / `clip-path` / `aapt:attr` + xmlns 前缀。
 */
internal fun parseXml(xml: String): Element {
    val parser = DomXmlParser()
    parser.parse(xml)
    return parser.root ?: throw MalformedXMLException("empty document")
}

internal fun ByteArray.toXmlElement(): Element =
    parseXml(decodeToString())

private class ElementImpl(
    override val localName: String,
    override val nodeName: String,
    override val namespaceURI: String,
    private val prefixMap: Map<String, String>,
    private val attributes: Map<String, String>,
) : Element {
    override var textContent: String? = null
    val children = mutableListOf<Node>()

    override val childNodes: NodeList
        get() = object : NodeList {
            override fun item(i: Int): Node = children[i]
            override val length: Int get() = children.size
        }

    override fun getAttributeNS(nameSpaceURI: String, localName: String): String {
        val prefix = prefixMap[nameSpaceURI]
        val attrKey = if (prefix.isNullOrEmpty()) localName else "$prefix:$localName"
        return getAttribute(attrKey)
    }

    override fun getAttribute(name: String): String = attributes[name] ?: ""

    override fun lookupPrefix(namespaceURI: String): String = prefixMap[namespaceURI] ?: ""
}

private class DomXmlParser {
    var root: ElementImpl? = null
        private set

    private val stack = ArrayDeque<ElementImpl>()
    /** namespaceURI → prefix（与上游 Apple DomXmlParser 一致）。 */
    private var prefixMap: Map<String, String> = emptyMap()
    private val prefixToUri = linkedMapOf<String, String>()

    fun parse(xml: String) {
        var i = 0
        val n = xml.length
        while (i < n) {
            when {
                xml.startsWith("<?", i) -> {
                    val end = xml.indexOf("?>", i)
                    if (end < 0) throw MalformedXMLException("unclosed PI")
                    i = end + 2
                }
                xml.startsWith("<!--", i) -> {
                    val end = xml.indexOf("-->", i)
                    if (end < 0) throw MalformedXMLException("unclosed comment")
                    i = end + 3
                }
                xml.startsWith("<![CDATA[", i) -> {
                    val end = xml.indexOf("]]>", i)
                    if (end < 0) throw MalformedXMLException("unclosed CDATA")
                    appendText(xml.substring(i + 9, end))
                    i = end + 3
                }
                xml[i] == '<' && i + 1 < n && xml[i + 1] == '/' -> {
                    val end = xml.indexOf('>', i)
                    if (end < 0) throw MalformedXMLException("unclosed end tag")
                    closeTag()
                    i = end + 1
                }
                xml[i] == '<' -> {
                    val end = xml.indexOf('>', i)
                    if (end < 0) throw MalformedXMLException("unclosed start tag")
                    val body = xml.substring(i + 1, end).trim()
                    val selfClosing = body.endsWith('/')
                    val tagBody = if (selfClosing) body.dropLast(1).trim() else body
                    openTag(tagBody)
                    if (selfClosing) closeTag()
                    i = end + 1
                }
                else -> {
                    val next = xml.indexOf('<', i).let { if (it < 0) n else it }
                    appendText(xml.substring(i, next))
                    i = next
                }
            }
        }
    }

    private fun appendText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        stack.lastOrNull()?.let { node ->
            node.textContent = node.textContent.orEmpty() + text
        }
    }

    private fun openTag(tagBody: String) {
        val tokens = tokenizeTag(tagBody)
        if (tokens.isEmpty()) throw MalformedXMLException("empty tag")
        val qName = tokens[0]
        val local = qName.substringAfterLast(':', qName)
        val attrs = linkedMapOf<String, String>()
        var ai = 1
        while (ai < tokens.size) {
            val name = tokens[ai]
            val eq = tokens.getOrNull(ai + 1)
            val value = tokens.getOrNull(ai + 2)
            if (eq != "=" || value == null) {
                // 容错：跳过残缺属性
                ai++
                continue
            }
            attrs[name] = value
            if (name == "xmlns") {
                prefixToUri[""] = value
            } else if (name.startsWith("xmlns:")) {
                prefixToUri[name.removePrefix("xmlns:")] = value
            }
            ai += 3
        }
        // 重建 URI→prefix（后声明覆盖）
        prefixMap = prefixToUri.entries.associate { (p, uri) -> uri to p }

        val nsUri = when {
            qName.contains(':') -> prefixToUri[qName.substringBefore(':')] ?: ""
            else -> prefixToUri[""] ?: ""
        }
        val node = ElementImpl(
            localName = local,
            nodeName = qName,
            namespaceURI = nsUri,
            prefixMap = prefixMap,
            attributes = attrs,
        )
        if (root == null) root = node
        stack.lastOrNull()?.children?.add(node)
        stack.addLast(node)
    }

    private fun closeTag() {
        if (stack.isEmpty()) throw MalformedXMLException("unexpected end tag")
        stack.removeLast()
    }

    /**
     * 把 `tag attr="v" xmlns:a="…"` 拆成 [name, "=", value, …]。
     * value 已去掉引号。
     */
    private fun tokenizeTag(body: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        val n = body.length
        fun skipWs() {
            while (i < n && body[i].isWhitespace()) i++
        }
        skipWs()
        // tag / qName
        if (i < n) {
            val start = i
            while (i < n && !body[i].isWhitespace() && body[i] != '=' && body[i] != '/') i++
            out += body.substring(start, i)
        }
        while (true) {
            skipWs()
            if (i >= n) break
            val nameStart = i
            while (i < n && !body[i].isWhitespace() && body[i] != '=') i++
            if (nameStart == i) break
            out += body.substring(nameStart, i)
            skipWs()
            if (i >= n || body[i] != '=') {
                out += "="
                out += ""
                continue
            }
            out += "="
            i++
            skipWs()
            if (i >= n) {
                out += ""
                break
            }
            val quote = body[i]
            if (quote == '"' || quote == '\'') {
                i++
                val vStart = i
                while (i < n && body[i] != quote) i++
                out += body.substring(vStart, i)
                if (i < n) i++
            } else {
                val vStart = i
                while (i < n && !body[i].isWhitespace() && body[i] != '/') i++
                out += body.substring(vStart, i)
            }
        }
        return out
    }
}
