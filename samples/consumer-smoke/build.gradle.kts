plugins {
    // app 插件会把 kotlin-gradle-plugin 拉进同一 classpath；此处勿再写 version，
    // 否则 Gradle：already on the classpath with an unknown version。
    id("com.composekn.app")
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
}

kotlin {
    linuxX64 {
        binaries.executable {
            entryPoint = "main.main"
        }
    }
    sourceSets {
        val linuxX64Main by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
    }
}
