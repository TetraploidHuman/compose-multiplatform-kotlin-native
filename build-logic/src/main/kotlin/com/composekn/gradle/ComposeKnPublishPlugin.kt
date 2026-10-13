package com.composekn.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType

/**
 * 给 compose-kn-* 挂 maven-publish（`com.composekn:<module>:<composekn.version>`）。
 *
 * 仓库：
 * - 本地 `build/maven-repo`
 * - 可选私服（`composekn.publish.url`）
 * - GitHub Packages（`-Pcomposekn.publish.github=true`）
 * - Maven Central / Central Portal（`-Pcomposekn.publish.central=true`，需凭证与签名）
 *
 * `-Pcomposekn.publish.skipMetadata=true`（发布脚本默认打开）：禁用全项目
 * `*KotlinMetadata` 任务，避开 skiko/compose-core 在 KN 2.4 下的
 * `@OptionalExpectation` 编译失败，仍可发 native klib。
 */
class ComposeKnPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        val version = project.findProperty("composekn.version")?.toString() ?: "0.5.67"
        project.group = project.findProperty("composekn.group")?.toString() ?: "com.composekn"
        project.version = version

        if (project.findProperty("composekn.publish.skipMetadata")?.toString() == "true") {
            project.rootProject.allprojects {
                tasks.configureEach {
                    if (name.contains("KotlinMetadata")) {
                        enabled = false
                    }
                }
            }
        }

        project.pluginManager.withPlugin("maven-publish") {
            project.extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "ComposeKnLocal"
                        url = project.uri(project.rootProject.layout.buildDirectory.dir("maven-repo"))
                    }
                    val remote = project.findProperty("composekn.publish.url")?.toString()
                    if (!remote.isNullOrBlank()) {
                        maven {
                            name = "ComposeKnRemote"
                            url = project.uri(remote)
                            credentialsFrom(project, this)
                        }
                    }
                    val ghToken = System.getenv("GITHUB_TOKEN")
                        ?: project.findProperty("composekn.publish.password")?.toString()
                    // 仅显式 -Pcomposekn.publish.github=true 才挂 GitHub Packages，
                    // 避免 CI 环境里有 GITHUB_TOKEN 时 :publish 误传 / 409 Conflict。
                    val wantGh = project.findProperty("composekn.publish.github")?.toString() == "true"
                    if (wantGh && !ghToken.isNullOrBlank()) {
                        val owner = project.findProperty("composekn.publish.github.owner")?.toString()
                            ?: System.getenv("GITHUB_REPOSITORY_OWNER")
                            ?: "TetraploidHuman"
                        val repo = project.findProperty("composekn.publish.github.repo")?.toString()
                            ?: System.getenv("GITHUB_REPOSITORY")?.substringAfter('/')
                            ?: "compose-multiplatform-kotlin-native"
                        maven {
                            name = "GitHubPackages"
                            url = project.uri("https://maven.pkg.github.com/$owner/$repo")
                            credentials {
                                username = project.findProperty("composekn.publish.user")?.toString()
                                    ?: System.getenv("GITHUB_ACTOR")
                                    ?: "github"
                                password = ghToken
                            }
                        }
                    }
                    // Maven Central（OSSRH staging / Central Portal maven 端点）。
                    // 需 composekn.publish.central.user|password + signing；默认不挂。
                    val wantCentral =
                        project.findProperty("composekn.publish.central")?.toString() == "true"
                    if (wantCentral) {
                        val centralUrl = project.findProperty("composekn.publish.central.url")
                            ?.toString()
                            ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                        maven {
                            name = "MavenCentral"
                            url = project.uri(centralUrl)
                            val u = project.findProperty("composekn.publish.central.user")?.toString()
                            val p = project.findProperty("composekn.publish.central.password")
                                ?.toString()
                            if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                                credentials {
                                    username = u
                                    password = p
                                }
                            }
                        }
                    }
                }
                publications.withType<MavenPublication>().configureEach {
                    pom {
                        name.set(project.name)
                        description.set(
                            project.findProperty("composekn.pom.description")?.toString()
                                ?: "ComposeKN: Compose Multiplatform on Kotlin/Native (linuxX64/mingwX64)",
                        )
                        url.set("https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native")
                        licenses {
                            license {
                                name.set("Apache-2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("composekn")
                                name.set("ComposeKN contributors")
                            }
                        }
                        scm {
                            url.set("https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native")
                            connection.set(
                                "scm:git:https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native.git",
                            )
                            developerConnection.set(
                                "scm:git:ssh://git@github.com/TetraploidHuman/compose-multiplatform-kotlin-native.git",
                            )
                        }
                    }
                }
            }
        }

        project.pluginManager.withPlugin("java-platform") {
            project.extensions.configure<PublishingExtension> {
                publications {
                    if (findByName("maven") == null) {
                        create<MavenPublication>("maven") {
                            from(project.components.getByName("javaPlatform"))
                            artifactId = project.name
                        }
                    }
                }
            }
        }
    }

    private fun credentialsFrom(
        project: Project,
        repo: org.gradle.api.artifacts.repositories.MavenArtifactRepository,
        userKey: String = "composekn.publish.user",
        passKey: String = "composekn.publish.password",
    ) {
        val u = project.findProperty(userKey)?.toString()
            ?: System.getenv("GITHUB_ACTOR")
        val p = project.findProperty(passKey)?.toString()
            ?: System.getenv("GITHUB_TOKEN")
        if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
            repo.credentials {
                username = u
                password = p
            }
        }
    }
}
