package cn.tetraploid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import java.io.File

/**
 * ComposeKN 资源管线（对齐官方 composeResources 布局的 native 子集）。
 *
 * 源：`src/commonMain/composeResources/{font,drawable,values,values-*}/`
 * 生成：`Res.font|string|plurals|drawable`（类型来自 `org.jetbrains.compose.resources`）；
 * 链接后 copy 到 exe 旁 `composeResources/`（含全部 `values-*` locale overlay）。
 *
 * DSL：`composeKnResources { }`；亦可用官方风格别名 [ComposeKnResourcesExtension.packageOfResClass]。
 */
class ComposeKnResourcesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create(
            "composeKnResources",
            ComposeKnResourcesExtension::class.java,
        )
        // 对齐官方默认：{group}.{module}.generated.resources；无 group 时退回稳定占位
        val inferredPkg = buildString {
            val g = project.group.toString().trim().trim('.')
            if (g.isNotEmpty() && g != "unspecified") append(g).append('.')
            append(project.name.replace('-', '_'))
            append(".generated.resources")
        }
        ext.packageName.convention(inferredPkg)
        // 仅当目录存在时再挂 InputDirectory，避免样本工程无 composeResources 时任务校验失败
        val defaultRes = project.file("src/commonMain/composeResources")
        if (defaultRes.isDirectory) {
            ext.resourcesDir.convention(
                project.layout.projectDirectory.dir("src/commonMain/composeResources"),
            )
        }
        ext.extraFontDirs.convention(emptyList())

        // 尽早注册 generate，便于 build.gradle 里 tasks.named(...) 挂依赖
        val genDir = project.layout.buildDirectory.dir("generated/composekn/resources")
        val generate = project.tasks.register(
            "generateComposeKnResources",
            GenerateComposeKnResourcesTask::class.java,
        ) {
            packageName.set(ext.packageName)
            resourcesDirectory.set(ext.resourcesDir)
            extraFontDirectories.set(ext.extraFontDirs)
            outputDirectory.set(genDir)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.afterEvaluate { configure(project, ext, generate, genDir) }
        }
    }

    private fun configure(
        project: Project,
        ext: ComposeKnResourcesExtension,
        generate: org.gradle.api.tasks.TaskProvider<GenerateComposeKnResourcesTask>,
        genDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
    ) {
        val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        kotlin.sourceSets.findByName("commonMain")?.let { ss ->
            ss.kotlin.srcDir(genDir)
            val inRepo = project.rootProject.findProject(":compose-kn-resources") != null
            val ver =
                project.findProperty("composekn.version")?.toString()
                    ?: ComposeKnHostPlugin.DEFAULT_VERSION
            ss.dependencies {
                if (inRepo) {
                    implementation(project.project(":compose-kn-resources"))
                } else {
                    implementation("cn.tetraploid:compose-kn-resources:$ver")
                }
            }
        }

        project.tasks.withType(KotlinNativeCompile::class.java).configureEach {
            dependsOn(generate)
        }

        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            val target = this
            binaries.withType(Executable::class.java).configureEach {
                val binary = this
                val copyName =
                    "copyComposeKnResources_${target.name}_${binary.buildType.getName()}"
                val copy = project.tasks.register(copyName) {
                    dependsOn(generate)
                    dependsOn(binary.linkTaskProvider)
                    doLast {
                        val outParent = binary.outputFile.parentFile
                        val rootOut = File(outParent, "composeResources")
                        val fontOut = File(rootOut, "font")
                        val drawableOut = File(rootOut, "drawable")
                        fontOut.mkdirs()
                        drawableOut.mkdirs()
                        val fontFiles = linkedMapOf<String, File>()
                        val drawableFiles = linkedMapOf<String, File>()
                        fun addFonts(dir: File?) {
                            if (dir == null || !dir.isDirectory) return
                            dir.listFiles()
                                ?.filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
                                ?.forEach { fontFiles[it.name] = it }
                        }
                        fun addDrawables(dir: File?) {
                            if (dir == null || !dir.isDirectory) return
                            dir.listFiles()
                                ?.filter {
                                    it.isFile && it.extension.lowercase() in setOf(
                                        "png", "webp", "jpg", "jpeg", "svg", "xml",
                                    )
                                }
                                ?.forEach { drawableFiles[it.name] = it }
                        }
                        val resRoot = ext.resourcesDir.orNull?.asFile
                        addFonts(resRoot?.resolve("font"))
                        addDrawables(resRoot?.resolve("drawable"))
                        ext.extraFontDirs.get().forEach { addFonts(File(it)) }
                        fontFiles.values.forEach { src ->
                            src.copyTo(File(fontOut, src.name), overwrite = true)
                        }
                        drawableFiles.values.forEach { src ->
                            src.copyTo(File(drawableOut, src.name), overwrite = true)
                        }
                        // values / values-zh / values-zh-rCN …
                        var valuesDirs = 0
                        resRoot?.listFiles()
                            ?.filter { it.isDirectory && (it.name == "values" || it.name.startsWith("values-")) }
                            ?.forEach { values ->
                                val valuesOut = File(rootOut, values.name)
                                valuesOut.mkdirs()
                                values.copyRecursively(valuesOut, overwrite = true)
                                valuesDirs++
                            }
                        logger.lifecycle(
                            "ComposeKN resources: copied fonts=${fontFiles.size} " +
                                "drawables=${drawableFiles.size} valuesDirs=$valuesDirs → $rootOut",
                        )
                    }
                }
                binary.linkTaskProvider.configure { finalizedBy(copy) }
            }
        }
    }
}

abstract class ComposeKnResourcesExtension {
    /** Res 类所在包（ComposeKN 原名）。 */
    abstract val packageName: Property<String>
    abstract val resourcesDir: DirectoryProperty
    abstract val extraFontDirs: ListProperty<String>

    /**
     * 官方 `compose.resources { packageOfResClass = "…" }` 同义别名，写入 [packageName]。
     */
    fun packageOfResClass(value: String) {
        packageName.set(value)
    }
}

abstract class GenerateComposeKnResourcesTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:InputDirectory
    @get:Optional
    abstract val resourcesDirectory: DirectoryProperty

    @get:Input
    abstract val extraFontDirectories: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val fonts = collectFonts(
            resourcesDirectory.orNull?.asFile,
            extraFontDirectories.get(),
        )
        val drawables = collectDrawables(resourcesDirectory.orNull?.asFile)
        val strings = collectStrings(resourcesDirectory.orNull?.asFile)
        val plurals = collectPlurals(resourcesDirectory.orNull?.asFile)
        val outRoot = outputDirectory.get().asFile
        outRoot.deleteRecursively()
        val pkg = packageName.get()
        val dir = File(outRoot, pkg.replace('.', '/'))
        dir.mkdirs()

        val fontProps = fonts.entries.sortedBy { it.key }.joinToString("\n") { (id, file) ->
            val rel = "font/${file.name}"
            val dev = file.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
            """
            |        val $id: FontResource = fontResource(
            |            identity = "$id",
            |            relativePath = "$rel",
            |            developmentPath = "$dev",
            |        )
            """.trimMargin()
        }
        val drawableProps = drawables.entries.sortedBy { it.key }.joinToString("\n") { (id, file) ->
            val rel = "drawable/${file.name}"
            val dev = file.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
            """
            |        val $id: DrawableResource = drawableResource(
            |            identity = "$id",
            |            relativePath = "$rel",
            |            developmentPath = "$dev",
            |        )
            """.trimMargin()
        }
        val stringProps = strings.entries.sortedBy { it.key }.joinToString("\n") { (id, value) ->
            val escaped = escapeKotlinString(value)
            """
            |        val $id: StringResource = stringResource(
            |            identity = "$id",
            |            relativePath = "values/strings.xml",
            |            defaultValue = "$escaped",
            |        )
            """.trimMargin()
        }
        val pluralProps = plurals.entries.sortedBy { it.key }.joinToString("\n") { (id, items) ->
            val mapLit = items.entries.joinToString(", ") { (q, v) ->
                "\"$q\" to \"${escapeKotlinString(v)}\""
            }
            """
            |        val $id: PluralStringResource = pluralStringResource(
            |            identity = "$id",
            |            relativePath = "values/strings.xml",
            |            items = mapOf($mapLit),
            |        )
            """.trimMargin()
        }

        File(dir, "Res.kt").writeText(
            """
            |package $pkg
            |
            |import org.jetbrains.compose.resources.DrawableResource
            |import org.jetbrains.compose.resources.FontResource
            |import org.jetbrains.compose.resources.PluralStringResource
            |import org.jetbrains.compose.resources.StringResource
            |import org.jetbrains.compose.resources.drawableResource
            |import org.jetbrains.compose.resources.fontResource
            |import org.jetbrains.compose.resources.pluralStringResource
            |import org.jetbrains.compose.resources.stringResource
            |
            |// ComposeKN 生成的资源入口（官方 compose.resources 心智）。
            |// 由 generateComposeKnResources 生成 —— 勿手改。
            |object Res {
            |    object font {
            |$fontProps
            |    }
            |    object string {
            |$stringProps
            |    }
            |    object plurals {
            |$pluralProps
            |    }
            |    object drawable {
            |$drawableProps
            |    }
            |}
            |
            """.trimMargin(),
        )

        logger.lifecycle(
            "ComposeKN resources: Res.font=${fonts.size} string=${strings.size} " +
                "plurals=${plurals.size} drawable=${drawables.size} → ${dir.resolve("Res.kt")}",
        )
    }

    companion object {
        fun collectFonts(resourcesRoot: File?, extraDirs: List<String>): Map<String, File> {
            val fonts = linkedMapOf<String, File>()
            fun addDir(dir: File?) {
                if (dir == null || !dir.isDirectory) return
                dir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
                    ?.sortedBy { it.name }
                    ?.forEach { fonts[it.nameWithoutExtension] = it }
            }
            addDir(resourcesRoot?.resolve("font"))
            extraDirs.forEach { addDir(File(it)) }
            return fonts
        }

        fun collectDrawables(resourcesRoot: File?): Map<String, File> {
            val out = linkedMapOf<String, File>()
            val dir = resourcesRoot?.resolve("drawable") ?: return out
            if (!dir.isDirectory) return out
            dir.listFiles()
                ?.filter {
                    it.isFile && it.extension.lowercase() in setOf(
                        "png", "webp", "jpg", "jpeg", "svg", "xml",
                    )
                }
                ?.sortedBy { it.name }
                ?.forEach { out[it.nameWithoutExtension] = it }
            return out
        }

        /** 极简 strings.xml：`<string name="x">y</string>`（仅默认 `values/`）。 */
        fun collectStrings(resourcesRoot: File?): Map<String, String> {
            val out = linkedMapOf<String, String>()
            for (name in listOf("strings.xml", "plurals.xml")) {
                val file = resourcesRoot?.resolve("values/$name") ?: continue
                if (!file.isFile) continue
                val re = Regex("""<string\s+name\s*=\s*"([^"]+)"\s*>([\s\S]*?)</string>""")
                re.findAll(file.readText()).forEach { m ->
                    out[m.groupValues[1]] = unescapeXml(m.groupValues[2].trim())
                }
            }
            return out
        }

        /** `<plurals name>` / `<item quantity>`（仅默认 `values/`）。 */
        fun collectPlurals(resourcesRoot: File?): Map<String, Map<String, String>> {
            val out = linkedMapOf<String, Map<String, String>>()
            for (name in listOf("strings.xml", "plurals.xml")) {
                val file = resourcesRoot?.resolve("values/$name") ?: continue
                if (!file.isFile) continue
                val block = Regex(
                    """<plurals\s+name\s*=\s*"([^"]+)"\s*>([\s\S]*?)</plurals>""",
                )
                val item = Regex(
                    """<item\s+quantity\s*=\s*"([^"]+)"\s*>([\s\S]*?)</item>""",
                )
                block.findAll(file.readText()).forEach { m ->
                    val quantities = linkedMapOf<String, String>()
                    item.findAll(m.groupValues[2]).forEach { im ->
                        quantities[im.groupValues[1]] = unescapeXml(im.groupValues[2].trim())
                    }
                    if (quantities.isNotEmpty()) out[m.groupValues[1]] = quantities
                }
            }
            return out
        }

        fun escapeKotlinString(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

        fun unescapeXml(s: String): String =
            s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
    }
}
