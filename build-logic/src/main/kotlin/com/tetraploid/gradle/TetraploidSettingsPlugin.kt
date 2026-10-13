package com.tetraploid.gradle

import org.gradle.api.Plugin
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.initialization.Settings
import java.io.File
import java.net.URI

/**
 * 消费者 settings 插件（也注册为 `com.tetraploid`）：
 * - 解析/缓存 Release Maven zip（稳定 CDN，默认 `~/.tetraploid/m2/<ver>`）
 * - 可选 GitHub Packages（需显式打开）
 * - 官方 Compose/Skiko/resources → `com.tetraploid` / `com.tetraploid.compose`；linuxX64/mingwX64 顶到平台 artifact
 *
 * 用户通常只需：
 * ```
 * // gradle.properties
 * tetraploid.version=0.5.68
 *
 * // settings.gradle.kts
 * plugins { id("com.tetraploid") version "0.5.68" }
 * ```
 * （`pluginManagement` 需能解析到插件 JAR：缓存目录、本机 publish 或 Packages。）
 */
class TetraploidSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val tetraploidVer = settings.providers.gradleProperty("tetraploid.version")
            .orElse(DEFAULT_VERSION).get()
        // 单版本：UI = <compose.ui>-tp.<tetraploid.version>，一般不用手写 ui.version
        val uiBase = settings.providers.gradleProperty("tetraploid.compose.ui")
            .orElse("1.12.1").get()
        val uiVer = settings.providers.gradleProperty("tetraploid.compose.ui.version")
            .orElse("$uiBase-tp.$tetraploidVer").get()
        val skipSub = settings.providers.gradleProperty("tetraploid.settings.skipSubstitution")
            .orNull == "true"
        val owner = settings.providers.gradleProperty("tetraploid.publish.github.owner")
            .orElse(settings.providers.environmentVariable("GITHUB_REPOSITORY_OWNER"))
            .orElse(TetraploidMavenCache.DEFAULT_OWNER).get()
        val repo = settings.providers.gradleProperty("tetraploid.publish.github.repo")
            .orElse(TetraploidMavenCache.DEFAULT_REPO).get()
        val token = settings.providers.environmentVariable("GITHUB_TOKEN")
            .orElse(settings.providers.environmentVariable("GH_TOKEN"))
            .orElse(settings.providers.gradleProperty("gpr.token"))
            .orNull
            ?.takeIf { it.isNotBlank() }

        val explicitLocal = settings.providers.gradleProperty("tetraploid.maven.local")
            .orElse(settings.providers.environmentVariable("TETRAPLOID_MAVEN_LOCAL"))
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val autoFetch = settings.providers.gradleProperty("tetraploid.maven.auto")
            .orElse("true").get() != "false"

        var resolvedMaven: File? = null
        if (explicitLocal != null) {
            val dir = File(explicitLocal)
            require(dir.isDirectory) {
                "tetraploid.maven.local is not a directory: $explicitLocal"
            }
            resolvedMaven = dir
        } else if (autoFetch) {
            val cacheProp = settings.providers.gradleProperty("tetraploid.maven.cache")
                .orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val dest = if (cacheProp != null) {
                File(cacheProp)
            } else {
                TetraploidMavenCache.defaultCacheDir(tetraploidVer)
            }
            resolvedMaven = TetraploidMavenCache.ensure(
                version = tetraploidVer,
                dest = dest,
                owner = owner,
                repo = repo,
                token = token,
                log = { msg -> println(msg) },
            )
            if (resolvedMaven == null) {
                System.err.println(
                    "Tetraploid: could not auto-fetch maven $tetraploidVer; " +
                        "set tetraploid.maven.local or run scripts/fetch-tetraploid-maven.sh",
                )
            }
        }

        if (resolvedMaven != null) {
            val uri = resolvedMaven.toURI()
            settings.dependencyResolutionManagement.repositories.maven {
                name = "TetraploidLocal"
                url = uri
            }
            settings.pluginManagement.repositories.maven {
                name = "TetraploidLocal"
                url = uri
            }
        }

        // Packages：显式 opt-in；或 Release 拉取失败且有 token 时自动回退
        val useGh = settings.providers.gradleProperty("tetraploid.settings.useGithubPackages")
            .orNull == "true" ||
            settings.providers.environmentVariable("TETRAPLOID_USE_GITHUB_PACKAGES")
                .orNull == "true"
        val explicitSkipGh = settings.providers.gradleProperty("tetraploid.settings.skipGithubPackages")
            .orNull == "true" ||
            settings.providers.environmentVariable("TETRAPLOID_SKIP_GITHUB_PACKAGES")
                .orNull == "true"
        val fallbackGh = resolvedMaven == null && !token.isNullOrBlank()
        val addGh = !explicitSkipGh && (useGh || fallbackGh)
        if (fallbackGh && !useGh && addGh) {
            println(
                "Tetraploid: Release cache missing; falling back to GitHub Packages " +
                    "(set tetraploid.settings.skipGithubPackages=true to disable)",
            )
        }

        if (addGh) {
            val ghUri = URI.create("https://maven.pkg.github.com/$owner/$repo")
            val user = settings.providers.environmentVariable("GITHUB_ACTOR")
                .orElse(settings.providers.gradleProperty("gpr.user"))
                .orElse("github")
            val ghToken = token ?: ""
            settings.dependencyResolutionManagement.repositories.maven {
                name = "TetraploidGitHubPackages"
                url = ghUri
                credentials {
                    username = user.get()
                    password = ghToken
                }
            }
            settings.pluginManagement.repositories.maven {
                name = "TetraploidGitHubPackages"
                url = ghUri
                credentials {
                    username = user.get()
                    password = ghToken
                }
            }
        }

        if (skipSub) return

        settings.gradle.beforeProject {
            configurations.configureEach {
                val confName = name
                val platform = nativePlatformHint(confName)
                resolutionStrategy.dependencySubstitution {
                    applyUiSubstitutions(this, uiVer, platform)
                    substitute(module("org.jetbrains.skiko:skiko"))
                        .using(module(skikoCoordinate(platform, tetraploidVer)))
                    substitute(module("org.jetbrains.compose.components:components-resources"))
                        .using(module(resourcesCoordinate(platform, tetraploidVer)))
                }
            }
        }
    }

    private fun nativePlatformHint(confName: String): String? {
        val lower = confName.lowercase()
        return when {
            lower.contains("linuxx64") || lower.contains("linux_x64") -> "linuxx64"
            lower.contains("mingwx64") || lower.contains("mingw_x64") -> "mingwx64"
            else -> null
        }
    }

    private fun skikoCoordinate(platform: String?, ver: String): String =
        when (platform) {
            "linuxx64" -> "com.tetraploid:skiko-linuxx64:$ver"
            "mingwx64" -> "com.tetraploid:skiko-mingwx64:$ver"
            else -> "com.tetraploid:skiko:$ver"
        }

    private fun resourcesCoordinate(platform: String?, ver: String): String =
        when (platform) {
            "linuxx64" -> "com.tetraploid:compose-kn-resources-linuxx64:$ver"
            "mingwx64" -> "com.tetraploid:compose-kn-resources-mingwx64:$ver"
            else -> "com.tetraploid:compose-kn-resources:$ver"
        }

    private fun applyUiSubstitutions(
        subs: DependencySubstitutions,
        uiVer: String,
        platform: String?,
    ) {
        fun coord(artifact: String): String =
            if (platform != null) {
                "com.tetraploid.compose:$artifact-$platform:$uiVer"
            } else {
                "com.tetraploid.compose:$artifact:$uiVer"
            }

        val uiModules = listOf(
            "ui-util", "ui-geometry", "ui-unit", "ui-graphics", "ui-text",
            "ui-backhandler", "ui",
        )
        for (m in uiModules) {
            subs.substitute(subs.module("org.jetbrains.compose.ui:$m"))
                .using(subs.module(coord(m)))
        }
        val other = listOf(
            "org.jetbrains.compose.foundation:foundation" to "foundation",
            "org.jetbrains.compose.foundation:foundation-layout" to "foundation-layout",
            "org.jetbrains.compose.animation:animation" to "animation",
            "org.jetbrains.compose.animation:animation-core" to "animation-core",
            "org.jetbrains.compose.material:material-ripple" to "material-ripple",
            "org.jetbrains.compose.material3:material3" to "material3",
        )
        for ((from, artifact) in other) {
            subs.substitute(subs.module(from))
                .using(subs.module(coord(artifact)))
        }
        val lifecycleArtifact = "lifecycle-viewmodel-compose"
        for (from in listOf(
            "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose",
            "androidx.lifecycle:lifecycle-viewmodel-compose",
        )) {
            subs.substitute(subs.module(from))
                .using(subs.module(coord(lifecycleArtifact)))
        }
    }

    companion object {
        const val DEFAULT_VERSION = "0.5.68"
    }
}
