package cn.tetraploid.resources

/**
 * 运行时解析 `composeResources/values(-*)/strings.xml` 与 `plurals.xml`。
 * 结果按目录缓存；[resourceLocaleOverride] 切换后仍可用已缓存的其它目录。
 */
internal object StringCatalog {
    private val stringCache = mutableMapOf<String, Map<String, String>>()
    private val pluralCache = mutableMapOf<String, Map<String, Map<String, String>>>()

    fun string(identity: String, locale: ResourceLocale): String? {
        for (dir in locale.valuesDirCandidates()) {
            loadStrings(dir)[identity]?.let { return it }
        }
        return null
    }

    fun plurals(identity: String, locale: ResourceLocale): Map<String, String>? {
        for (dir in locale.valuesDirCandidates()) {
            loadPlurals(dir)[identity]?.let { return it }
        }
        return null
    }

    private fun loadStrings(valuesDir: String): Map<String, String> =
        stringCache.getOrPut(valuesDir) {
            val merged = linkedMapOf<String, String>()
            for (fileName in listOf("strings.xml", "plurals.xml")) {
                val bytes = loadValuesFile(valuesDir, fileName) ?: continue
                parseStringEntries(bytes.decodeToString(), merged)
            }
            merged
        }

    private fun loadPlurals(valuesDir: String): Map<String, Map<String, String>> =
        pluralCache.getOrPut(valuesDir) {
            val merged = linkedMapOf<String, Map<String, String>>()
            for (fileName in listOf("strings.xml", "plurals.xml")) {
                val bytes = loadValuesFile(valuesDir, fileName) ?: continue
                parsePluralEntries(bytes.decodeToString(), merged)
            }
            merged
        }

    private fun loadValuesFile(valuesDir: String, fileName: String): ByteArray? {
        val rel = "$valuesDir/$fileName"
        return ResourceLoads.loadBytes(
            FontResource(identity = rel, relativePath = rel, developmentPath = null),
        )
    }
}

/** `<string name="x">y</string>` */
internal fun parseStringEntries(xml: String, out: MutableMap<String, String>) {
    val re = Regex("""<string\s+name\s*=\s*"([^"]+)"\s*>([\s\S]*?)</string>""")
    re.findAll(xml).forEach { m ->
        out[m.groupValues[1]] = unescapeXml(m.groupValues[2].trim())
    }
}

/**
 * ```
 * <plurals name="items">
 *   <item quantity="one">%d item</item>
 *   <item quantity="other">%d items</item>
 * </plurals>
 * ```
 */
internal fun parsePluralEntries(xml: String, out: MutableMap<String, Map<String, String>>) {
    val block = Regex(
        """<plurals\s+name\s*=\s*"([^"]+)"\s*>([\s\S]*?)</plurals>""",
    )
    val item = Regex(
        """<item\s+quantity\s*=\s*"([^"]+)"\s*>([\s\S]*?)</item>""",
    )
    block.findAll(xml).forEach { m ->
        val name = m.groupValues[1]
        val body = m.groupValues[2]
        val quantities = linkedMapOf<String, String>()
        item.findAll(body).forEach { im ->
            quantities[im.groupValues[1]] = unescapeXml(im.groupValues[2].trim())
        }
        if (quantities.isNotEmpty()) out[name] = quantities
    }
}

internal fun unescapeXml(s: String): String =
    s.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("\\n", "\n")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
