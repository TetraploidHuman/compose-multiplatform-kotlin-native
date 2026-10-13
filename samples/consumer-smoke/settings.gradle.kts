pluginManagement {
    val localMaven = providers.gradleProperty("composekn.maven.local")
        .orElse(providers.environmentVariable("COMPOSEKN_MAVEN_LOCAL"))
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val cknVer = providers.gradleProperty("composekn.version").orElse("0.5.67").get()
    // 与 settings 插件默认缓存一致；首次需已 fetch 或指向 local，才能解析插件 JAR
    val cacheMaven = java.io.File(
        System.getProperty("user.home"),
        ".composekn/m2/$cknVer",
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
            if (requested.id.id.startsWith("com.composekn")) {
                useVersion(cknVer)
            }
        }
    }
}

plugins {
    id("com.composekn")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "consumer-smoke"
