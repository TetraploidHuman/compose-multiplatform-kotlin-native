package com.tetraploid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 消费方一键应用插件：host（linker + 后端登记）+ resources。
 *
 * ```
 * plugins {
 *   id("com.tetraploid.app")
 *   kotlin("multiplatform") // no version — provided via plugin classpath
 *   id("org.jetbrains.compose")
 * }
 * ```
 *
 * settings 侧配合 `id("com.tetraploid")`（或 `com.tetraploid.settings`）。
 * 本仓库内开发时自动 `project()` 依赖宿主；外部 Maven 消费则走坐标。
 */
class TetraploidAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.tetraploid.host")
        project.pluginManager.apply("com.tetraploid.resources")

        val inTetraploidRepo =
            project.rootProject.findProject(":compose-kn-linux") != null
        project.extensions.configure(TetraploidHostExtension::class.java) {
            useProjectDependencies.convention(inTetraploidRepo)
            val ver = project.findProperty("tetraploid.version")?.toString()
            if (!ver.isNullOrBlank()) {
                tetraploidVersion.convention(ver)
            }
        }
    }
}
