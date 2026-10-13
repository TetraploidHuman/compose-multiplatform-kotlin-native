plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("cn.tetraploid.publish")
}

kotlin {
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.skiko:skiko:0.0.0")
                implementation(libs.compose.ui)
            }
        }
    }
}
