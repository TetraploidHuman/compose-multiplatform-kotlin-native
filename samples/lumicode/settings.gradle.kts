pluginManagement {
    val localMaven = providers.gradleProperty("composekn.maven.local")
        .orElse(providers.environmentVariable("COMPOSEKN_MAVEN_LOCAL"))
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val cknVer = providers.gradleProperty("composekn.version").orElse("0.5.70").get()
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
            if (requested.id.id.startsWith("cn.tetraploid")) {
                useVersion(cknVer)
            }
        }
    }
}

plugins {
    id("cn.tetraploid")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "lumicode"
