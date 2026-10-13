plugins {
    // Maven 消费：host + resources；kotlin-gradle-plugin 已在 classpath
    id("cn.tetraploid.app")
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
}

val lumicodeRoot = file("../../../LumiCodeNext/composeApp")
val lumicodeCommon = file("$lumicodeRoot/src/commonMain/kotlin")
val lumicodeFonts = file("$lumicodeRoot/src/commonMain/composeResources/font")

composeKnResources {
    packageOfResClass("com.lumicode.editor.resources")
    extraFontDirs.add(lumicodeFonts.absolutePath)
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "main.main"
                val mingwLibs = (project.findProperty("skiko.mingw.libs") as? String)
                if (!mingwLibs.isNullOrBlank()) {
                    linkerOpts.addAll(
                        mingwLibs.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    )
                }
                val mingwLibDirs = (project.findProperty("skiko.mingw.libDirs") as? String)
                if (!mingwLibDirs.isNullOrBlank()) {
                    linkerOpts.addAll(
                        mingwLibDirs.split(',', ':').map { "-L${it.trim()}" }.filter { it != "-L" },
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(lumicodeCommon)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            }
        }

        val linuxX64Main by getting {
            kotlin.srcDir("src/knShared/kotlin")
        }

        val mingwX64Main by getting {
            kotlin.srcDir("src/knShared/kotlin")
        }
    }
}

tasks.register("verifyLumicodeSources") {
    doLast {
        check(lumicodeCommon.isDirectory) {
            "LumiCode commonMain not found at $lumicodeCommon — clone/checkout LumiCodeNext next to compose-multiplatform-kotlin-native"
        }
        check(lumicodeFonts.isDirectory) {
            "LumiCode fonts not found at $lumicodeFonts"
        }
    }
}
tasks.named("compileKotlinLinuxX64") { dependsOn("verifyLumicodeSources") }
tasks.matching { it.name.startsWith("compileKotlinMingw") }.configureEach {
    dependsOn("verifyLumicodeSources")
}
tasks.named("generateComposeKnResources") { dependsOn("verifyLumicodeSources") }

configurations.configureEach {
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
}
