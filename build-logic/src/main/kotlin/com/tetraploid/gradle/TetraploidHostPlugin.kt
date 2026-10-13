package com.tetraploid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import java.io.File

/**
 * 一键宿主：检测到 linuxX64 / mingwX64 后自动
 * - apply linker 插件
 * - 依赖 compose-kn-linux / compose-kn-windows
 * - 生成 entry wrapper：先 registerBackend，再调用户原来的 entryPoint
 *
 * Linux / Windows 生成源分目录，避免双 target 时互相污染编译。
 *
 * 用法：`id("com.tetraploid.host")`
 */
class TetraploidHostPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("tetraploidHost", TetraploidHostExtension::class.java)
        ext.useProjectDependencies.convention(true)
        ext.tetraploidVersion.convention(
            project.findProperty("tetraploid.version")?.toString() ?: DEFAULT_VERSION,
        )
        ext.wrapEntryPoint.convention(true)

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.afterEvaluate { configure(project, ext) }
        }
    }

    private fun configure(project: Project, ext: TetraploidHostExtension) {
        // afterEvaluate 偶发重复；同一工程只配置一次
        val flag = "tetraploid.host.configured"
        if (project.extensions.extraProperties.has(flag)) return
        project.extensions.extraProperties.set(flag, true)

        val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val hasLinux = kotlin.targets.findByName("linuxX64") != null
        val hasMingw = kotlin.targets.findByName("mingwX64") != null
        if (!hasLinux && !hasMingw) {
            project.logger.lifecycle("Tetraploid host: no linuxX64/mingwX64 target — skip")
            return
        }

        if (hasLinux) project.pluginManager.apply("com.tetraploid.linux-native-linker")
        if (hasMingw) project.pluginManager.apply("com.tetraploid.windows-native-linker")

        val linuxGen = project.layout.buildDirectory.dir("generated/tetraploid/host/linux")
        val windowsGen = project.layout.buildDirectory.dir("generated/tetraploid/host/windows")

        // target → 用户 entryPoint（每个平台只记一次，避免 debug/release 双 binary 叠 doLast）
        val wrapByPlatform = linkedMapOf<String, String>()

        val generate = project.tasks.register("generateTetraploidHostBootstrap") {
            outputs.dir(linuxGen)
            outputs.dir(windowsGen)
            inputs.property("wrap", ext.wrapEntryPoint)
            doLast {
                if (hasLinux) {
                    val dir = linuxGen.get().asFile
                    dir.mkdirs()
                    writeBootstrap(
                        File(dir, "TetraploidLinuxBootstrap.kt"),
                        platform = "linux",
                        importRegister = "com.tetraploid.linux.registerTetraploidLinuxBackend",
                        importInstall = "com.tetraploid.linux.installTetraploidLinuxAutoRegister",
                        registerCall = "registerTetraploidLinuxBackend()",
                        installCall = "installTetraploidLinuxAutoRegister()",
                    )
                }
                if (hasMingw) {
                    val dir = windowsGen.get().asFile
                    dir.mkdirs()
                    writeBootstrap(
                        File(dir, "TetraploidWindowsBootstrap.kt"),
                        platform = "windows",
                        importRegister = "com.tetraploid.windows.registerTetraploidWindowsBackend",
                        importInstall = "com.tetraploid.windows.installTetraploidWindowsAutoRegister",
                        registerCall = "registerTetraploidWindowsBackend()",
                        installCall = "installTetraploidWindowsAutoRegister()",
                    )
                }
                for ((platform, userFq) in wrapByPlatform) {
                    val isLinux = platform == "linux"
                    val dir = (if (isLinux) linuxGen else windowsGen).get().asFile
                    dir.mkdirs()
                    val fileName =
                        if (isLinux) "TetraploidLinuxEntry.kt" else "TetraploidWindowsEntry.kt"
                    val funName =
                        if (isLinux) "tetraploidLinuxHostMain" else "tetraploidWindowsHostMain"
                    val registerImport = if (isLinux) {
                        "com.tetraploid.linux.registerTetraploidLinuxBackend"
                    } else {
                        "com.tetraploid.windows.registerTetraploidWindowsBackend"
                    }
                    val registerCall = if (isLinux) {
                        "registerTetraploidLinuxBackend()"
                    } else {
                        "registerTetraploidWindowsBackend()"
                    }
                    File(dir, fileName).writeText(
                        """
                        |package tetraploid.host.generated
                        |
                        |import $registerImport
                        |
                        |/** 由 com.tetraploid.host 生成：先登记后端，再进入用户 entryPoint。 */
                        |fun $funName(args: Array<String> = emptyArray()) {
                        |    $registerCall
                        |    $userFq(args)
                        |}
                        |
                        """.trimMargin(),
                    )
                }
            }
        }

        project.tasks.withType(KotlinNativeCompile::class.java).configureEach {
            dependsOn(generate)
        }

        fun addDep(
            sourceSetName: String,
            projectPath: String,
            mavenArtifact: String,
            genDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        ) {
            kotlin.sourceSets.findByName(sourceSetName)?.let { ss ->
                ss.kotlin.srcDir(genDir)
                ss.dependencies {
                    if (ext.useProjectDependencies.get() &&
                        project.rootProject.findProject(projectPath) != null
                    ) {
                        implementation(project.project(projectPath))
                    } else {
                        implementation("com.tetraploid:$mavenArtifact:${ext.tetraploidVersion.get()}")
                    }
                }
            }
        }
        if (hasLinux) {
            addDep("linuxX64Main", ":compose-kn-linux", "compose-kn-linux", linuxGen)
        }
        if (hasMingw) {
            addDep("mingwX64Main", ":compose-kn-windows", "compose-kn-windows", windowsGen)
        }

        registerRunTasks(project, kotlin, hasLinux, hasMingw)

        if (!ext.wrapEntryPoint.get()) return

        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            val target = this
            val isLinux = target.name == "linuxX64"
            val isMingw = target.name == "mingwX64"
            if (!isLinux && !isMingw) return@configureEach
            val platform = if (isLinux) "linux" else "windows"
            binaries.withType(Executable::class.java).configureEach {
                val userEntry = entryPoint ?: return@configureEach
                if (userEntry.startsWith("tetraploid.host.generated.")) return@configureEach
                val wrapperName = if (isLinux) {
                    "tetraploid.host.generated.tetraploidLinuxHostMain"
                } else {
                    "tetraploid.host.generated.tetraploidWindowsHostMain"
                }
                if (wrapByPlatform.putIfAbsent(platform, userEntry) == null) {
                    project.logger.lifecycle(
                        "Tetraploid host: ${target.name} entryPoint $userEntry → $wrapperName",
                    )
                }
                entryPoint = wrapperName
            }
        }
    }

    /**
     * `runLinuxX64` / `runMingwX64`：链接 release executable 后启动。
     * Linux 优先走仓库内 `scripts/run-linux-native.sh`（NixOS / Wayland 库路径）；
     * 否则直接 exec，并设置 `WAYLAND_DISPLAY`。
     */
    private fun registerRunTasks(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        hasLinux: Boolean,
        hasMingw: Boolean,
    ) {
        fun registerFor(targetName: String, taskName: String) {
            val target = kotlin.targets.findByName(targetName) as? KotlinNativeTarget ?: return
            val release = target.binaries.find {
                it is Executable && it.buildType == NativeBuildType.RELEASE
            } as? Executable ?: return
            val linkTaskName = release.linkTaskName
            project.tasks.register(taskName, Exec::class.java) {
                group = "tetraploid"
                description = "Link and run the $targetName release executable"
                val stableName = linkTaskName + "Stable"
                dependsOn(
                    project.provider {
                        if (project.tasks.findByName(stableName) != null) stableName
                        else linkTaskName
                    },
                )
                isIgnoreExitValue = false
                doFirst {
                    val output = release.outputFile
                    if (!output.exists()) {
                        error("Executable not found: $output (task $linkTaskName)")
                    }
                    if (targetName == "linuxX64") {
                        val helper = project.rootProject.file("scripts/run-linux-native.sh")
                        if (helper.isFile) {
                            executable = "bash"
                            args(helper.absolutePath, output.absolutePath)
                        } else {
                            environment(
                                "WAYLAND_DISPLAY",
                                System.getenv("WAYLAND_DISPLAY") ?: "wayland-0",
                            )
                            if (System.getenv("XDG_RUNTIME_DIR").isNullOrBlank()) {
                                environment("XDG_RUNTIME_DIR", "/run/user/${posixUid()}")
                            }
                            executable = output.absolutePath
                        }
                    } else {
                        executable = output.absolutePath
                    }
                }
            }
        }
        if (hasLinux) registerFor("linuxX64", "runLinuxX64")
        if (hasMingw) registerFor("mingwX64", "runMingwX64")
    }

    private fun writeBootstrap(
        file: File,
        platform: String,
        importRegister: String,
        importInstall: String,
        registerCall: String,
        installCall: String,
    ) {
        file.writeText(
            """
            |package tetraploid.host.generated
            |
            |import $importInstall
            |import $importRegister
            |
            |@Suppress("unused")
            |internal val tetraploid${platform.replaceFirstChar { it.uppercase() }}HostBootstrap: Boolean =
            |    run {
            |        $installCall
            |        $registerCall
            |        true
            |    }
            |
            """.trimMargin(),
        )
    }

    private fun posixUid(): String =
        try {
            ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readText().trim()
                .ifBlank { "1000" }
        } catch (_: Exception) {
            "1000"
        }

    companion object {
        const val DEFAULT_VERSION = "0.5.68"
    }
}

abstract class TetraploidHostExtension {
    /** true：本仓库内用 project()；false：走 Maven `com.tetraploid:compose-kn-*` */
    abstract val useProjectDependencies: Property<Boolean>
    abstract val tetraploidVersion: Property<String>
    /** true：把 executable entryPoint 包一层自动 register（推荐） */
    abstract val wrapEntryPoint: Property<Boolean>
}
