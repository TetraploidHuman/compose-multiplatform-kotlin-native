package com.tetraploid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType

/**
 * 给 compose-kn-* 挂 maven-publish（`com.tetraploid:<module>:<tetraploid.version>`）。
 *
 * 仓库：
 * - 本地 `build/maven-repo`
 * - 可选私服（`tetraploid.publish.url`）
 * - GitHub Packages（`-Ptetraploid.publish.github=true`）
 * - Maven Central / Central Portal（`-Ptetraploid.publish.central=true`，需凭证与签名）
 *
 * `-Ptetraploid.publish.skipMetadata=true`（发布脚本默认打开）：禁用全项目
 * `*KotlinMetadata` 任务，避开 skiko/compose-core 在 KN 2.4 下的
 * `@OptionalExpectation` 编译失败，仍可发 native klib。
 */
class TetraploidPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        val version = project.findProperty("tetraploid.version")?.toString() ?: "0.5.68"
        project.group = project.findProperty("tetraploid.group")?.toString() ?: "com.tetraploid"
        project.version = version

        if (project.findProperty("tetraploid.publish.skipMetadata")?.toString() == "true") {
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
                        name = "TetraploidLocal"
                        url = project.uri(project.rootProject.layout.buildDirectory.dir("maven-repo"))
                    }
                    val remote = project.findProperty("tetraploid.publish.url")?.toString()
                    if (!remote.isNullOrBlank()) {
                        maven {
                            name = "TetraploidRemote"
                            url = project.uri(remote)
                            credentialsFrom(project, this)
                        }
                    }
                    val ghToken = System.getenv("GITHUB_TOKEN")
                        ?: project.findProperty("tetraploid.publish.password")?.toString()
                    // 仅显式 -Ptetraploid.publish.github=true 才挂 GitHub Packages，
                    // 避免 CI 环境里有 GITHUB_TOKEN 时 :publish 误传 / 409 Conflict。
                    val wantGh = project.findProperty("tetraploid.publish.github")?.toString() == "true"
                    if (wantGh && !ghToken.isNullOrBlank()) {
                        val owner = project.findProperty("tetraploid.publish.github.owner")?.toString()
                            ?: System.getenv("GITHUB_REPOSITORY_OWNER")
                            ?: "TetraploidHuman"
                        val repo = project.findProperty("tetraploid.publish.github.repo")?.toString()
                            ?: System.getenv("GITHUB_REPOSITORY")?.substringAfter('/')
                            ?: "compose-multiplatform-kotlin-native"
                        maven {
                            name = "GitHubPackages"
                            url = project.uri("https://maven.pkg.github.com/$owner/$repo")
                            credentials {
                                username = project.findProperty("tetraploid.publish.user")?.toString()
                                    ?: System.getenv("GITHUB_ACTOR")
                                    ?: "github"
                                password = ghToken
                            }
                        }
                    }
                    // Maven Central（OSSRH staging / Central Portal maven 端点）。
                    // 需 tetraploid.publish.central.user|password + signing；默认不挂。
                    val wantCentral =
                        project.findProperty("tetraploid.publish.central")?.toString() == "true"
                    if (wantCentral) {
                        val centralUrl = project.findProperty("tetraploid.publish.central.url")
                            ?.toString()
                            ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                        maven {
                            name = "MavenCentral"
                            url = project.uri(centralUrl)
                            val u = project.findProperty("tetraploid.publish.central.user")?.toString()
                            val p = project.findProperty("tetraploid.publish.central.password")
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
                            project.findProperty("tetraploid.pom.description")?.toString()
                                ?: "compose-multiplatform-kotlin-native: Compose Multiplatform on Kotlin/Native (linuxX64/mingwX64)",
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
                                id.set("tetraploid")
                                name.set("TetraploidHuman")
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
        userKey: String = "tetraploid.publish.user",
        passKey: String = "tetraploid.publish.password",
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
