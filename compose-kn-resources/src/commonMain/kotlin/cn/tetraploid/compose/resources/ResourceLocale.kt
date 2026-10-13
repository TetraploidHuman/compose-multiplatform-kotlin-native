package cn.tetraploid.compose.resources

/**
 * 资源语言标签（对齐官方 ResourceEnvironment 的 language + region 子集）。
 *
 * 解析顺序：[COMPOSEKN_LOCALE] → `LC_ALL`/`LC_MESSAGES`/`LANG` → [systemLocaleTag]。
 * 形如 `zh-CN` / `zh_CN` / `zh_CN.UTF-8`。
 */
data class ResourceLocale(
    val language: String,
    val region: String? = null,
) {
    /** `values` 目录候选：`values-zh-rCN` → `values-zh`。 */
    fun valuesDirCandidates(): List<String> {
        val lang = language.lowercase()
        if (lang.isEmpty() || lang == "und") return emptyList()
        val out = ArrayList<String>(2)
        val reg = region?.uppercase()?.takeIf { it.isNotEmpty() }
        if (reg != null) out += "values-$lang-r$reg"
        out += "values-$lang"
        return out
    }
}

/** 当前进程资源 locale（可变：测试 / 运行时切换）。 */
var resourceLocaleOverride: ResourceLocale? = null

fun currentResourceLocale(): ResourceLocale {
    resourceLocaleOverride?.let { return it }
    parseLocaleTag(getenv("COMPOSEKN_LOCALE"))?.let { return it }
    parseLocaleTag(getenv("LC_ALL")?.takeUnless { it == "C" || it == "POSIX" })?.let { return it }
    parseLocaleTag(getenv("LC_MESSAGES")?.takeUnless { it == "C" || it == "POSIX" })?.let { return it }
    parseLocaleTag(getenv("LANG")?.takeUnless { it == "C" || it == "POSIX" })?.let { return it }
    parseLocaleTag(systemLocaleTag())?.let { return it }
    return ResourceLocale("en", null)
}

internal fun parseLocaleTag(raw: String?): ResourceLocale? {
    if (raw.isNullOrBlank()) return null
    var s = raw.trim()
    // strip encoding: zh_CN.UTF-8
    s = s.substringBefore('.')
    // LANGUAGE=zh_CN:en_US → first
    s = s.substringBefore(':')
    if (s.isEmpty() || s.equals("C", ignoreCase = true) || s.equals("POSIX", ignoreCase = true)) {
        return null
    }
    val normalized = s.replace('-', '_')
    val parts = normalized.split('_').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    val language = parts[0].lowercase()
    if (language.length !in 2..8) return null
    val region = parts.getOrNull(1)?.uppercase()?.takeIf { it.length in 2..3 }
    return ResourceLocale(language, region)
}

/** 平台默认 locale 标签（可空）。 */
internal expect fun systemLocaleTag(): String?
