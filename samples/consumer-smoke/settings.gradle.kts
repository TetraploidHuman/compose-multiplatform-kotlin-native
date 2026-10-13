pluginManagement {
    val localMaven = providers.gradleProperty("tetraploid.maven.local")
        .orElse(providers.environmentVariable("TETRAPLOID_MAVEN_LOCAL"))
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val tpVer = providers.gradleProperty("tetraploid.version").orElse("0.5.68").get()
    // 与 settings 插件默认缓存一致；首次需已 fetch 或指向 local，才能解析插件 JAR
    val cacheMaven = java.io.File(
        System.getProperty("user.home"),
        ".tetraploid/m2/$tpVer",
    )
    repositories {
        if (localMaven != null) {
            maven { url = uri(localMaven) }
        } else if (cacheMaven.isDirectory) {
            maven { url = uri(cacheMaven) }
        }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.tetraploid")) {
                useVersion(tpVer)
            }
        }
    }
}

plugins {
    id("com.tetraploid")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "consumer-smoke"
