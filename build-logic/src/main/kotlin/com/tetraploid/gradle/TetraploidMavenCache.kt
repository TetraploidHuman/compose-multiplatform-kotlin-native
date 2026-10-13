package com.tetraploid.gradle

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

/**
 * 把 GitHub Release 里的 `tetraploid-maven-<ver>.zip` 落到本地缓存，
 * 作为免 Packages Billing 的稳定 Maven 源（「CDN」路径）。
 *
 * 默认目录：`~/.tetraploid/m2/<version>/`
 */
object TetraploidMavenCache {
    const val DEFAULT_OWNER = "TetraploidHuman"
    const val DEFAULT_REPO = "compose-multiplatform-kotlin-native"

    fun defaultCacheDir(version: String): File =
        File(System.getProperty("user.home"), ".tetraploid/m2/$version")

    fun isPopulated(dir: File): Boolean =
        File(dir, "com/tetraploid").isDirectory &&
            (
                File(dir, ".tetraploid-complete").isFile ||
                    File(dir, "com/tetraploid/tetraploid-build-logic").isDirectory
                )

    /**
     * 若 [dest] 已有构件则直接返回；否则尝试下载 Release zip 并解压。
     * @return 可用目录，失败时 null（调用方可回退 Packages / 显式 local）
     */
    fun ensure(
        version: String,
        dest: File = defaultCacheDir(version),
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO,
        token: String? = null,
        log: (String) -> Unit = {},
    ): File? {
        if (isPopulated(dest)) {
            log("Tetraploid maven cache hit: $dest")
            return dest
        }
        dest.parentFile?.mkdirs()
        val asset = "tetraploid-maven-$version.zip"
        val tag = "v$version"
        val staging = File(dest.parentFile, "${dest.name}.staging-${ProcessHandle.current().pid()}")
        return try {
            log("Tetraploid: fetching $owner/$repo@$tag /$asset → $dest")
            if (staging.exists()) staging.deleteRecursively()
            staging.mkdirs()
            val zipFile = File(staging, asset)
            downloadReleaseAsset(owner, repo, tag, asset, token, zipFile)
            unzip(zipFile, staging)
            zipFile.delete()
            File(staging, ".tetraploid-complete").writeText(version)
            if (!isPopulated(staging)) {
                log("Tetraploid: unzipped but com/tetraploid missing under $staging")
                staging.deleteRecursively()
                null
            } else {
                if (dest.exists()) dest.deleteRecursively()
                try {
                    Files.move(staging.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } catch (_: Exception) {
                    Files.move(staging.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                log("Tetraploid maven cache ready: $dest")
                dest
            }
        } catch (e: Exception) {
            log("Tetraploid: auto-fetch failed (${e.message})")
            runCatching { staging.deleteRecursively() }
            null
        }
    }

    private fun downloadReleaseAsset(
        owner: String,
        repo: String,
        tag: String,
        asset: String,
        token: String?,
        destFile: File,
    ) {
        val browser =
            "https://github.com/$owner/$repo/releases/download/$tag/$asset"
        if (tryDownloadTo(browser, token, destFile)) return

        val api =
            "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"
        val metaFile = File(destFile.parentFile, "release.json")
        if (!tryDownloadTo(api, token, metaFile, accept = "application/vnd.github+json")) {
            error("cannot read release $tag")
        }
        val meta = metaFile.readText()
        metaFile.delete()
        val id = findReleaseAssetId(meta, asset)
            ?: error("asset $asset not in release $tag")
        val assetUrl =
            "https://api.github.com/repos/$owner/$repo/releases/assets/$id"
        if (!tryDownloadTo(assetUrl, token, destFile, accept = "application/octet-stream")) {
            error("download asset $id failed")
        }
    }

    /**
     * 在 release JSON 的 `assets` 数组里找名为 [asset] 的 id（不依赖字段顺序）。
     */
    internal fun findReleaseAssetId(json: String, asset: String): String? {
        val assetsKey = json.indexOf("\"assets\"")
        if (assetsKey < 0) return null
        val arrayStart = json.indexOf('[', assetsKey)
        if (arrayStart < 0) return null
        val arrayEnd = findMatchingBracket(json, arrayStart, '[', ']') ?: return null
        val assets = json.substring(arrayStart, arrayEnd + 1)

        var searchFrom = 0
        val nameKey = "\"name\""
        while (true) {
            val nameIdx = assets.indexOf(nameKey, searchFrom)
            if (nameIdx < 0) return null
            val afterName = assets.indexOf(':', nameIdx + nameKey.length)
            if (afterName < 0) return null
            val valueStart = assets.indexOf('"', afterName + 1)
            if (valueStart < 0) return null
            val valueEnd = assets.indexOf('"', valueStart + 1)
            if (valueEnd < 0) return null
            val name = assets.substring(valueStart + 1, valueEnd)
            searchFrom = valueEnd + 1
            if (name != asset) continue
            val objStart = assets.lastIndexOf('{', nameIdx)
            val objEnd = assets.indexOf('}', valueEnd).let { if (it < 0) assets.length else it }
            if (objStart < 0) continue
            val slice = assets.substring(objStart, objEnd + 1)
            val idMatch = Regex(""""id"\s*:\s*(\d+)""").find(slice) ?: continue
            return idMatch.groupValues[1]
        }
    }

    private fun findMatchingBracket(
        s: String,
        openIdx: Int,
        open: Char,
        close: Char,
    ): Int? {
        var depth = 0
        var i = openIdx
        while (i < s.length) {
            when (s[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return null
    }

    private fun tryDownloadTo(
        url: String,
        token: String?,
        destFile: File,
        accept: String? = null,
    ): Boolean {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 600_000
        conn.setRequestProperty("User-Agent", "Tetraploid-MavenCache")
        if (accept != null) conn.setRequestProperty("Accept", accept)
        if (!token.isNullOrBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            if (conn.responseCode !in 200..299) false
            else {
                destFile.parentFile?.mkdirs()
                conn.inputStream.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        } catch (_: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun unzip(zipFile: File, dest: File) {
        val destCanon = dest.canonicalFile
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(dest, entry.name).canonicalFile
                if (!out.path.startsWith(destCanon.path + File.separator) && out != destCanon) {
                    error("refusing zip slip entry: ${entry.name}")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { copy(zis, it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun copy(input: InputStream, output: java.io.OutputStream) {
        input.copyTo(output)
    }
}
