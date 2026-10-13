plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.composekn"
version = providers.gradleProperty("composekn.version").orElse("0.5.67").get()

dependencies {
    implementation(gradleApi())
    // Must be implementation (not compileOnly): Maven consumers load host/resources plugins
    // in an isolated classloader; decorating ComposeKnHostPlugin needs KMP types on that path.
    // includeBuild hid this because the composite classpath already had KGP.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("konanLinkWorkaround") {
            id = "com.composekn.konan-link-workaround"
            implementationClass = "com.composekn.gradle.KonanLinkWorkaroundPlugin"
        }
        create("linuxNativeLinker") {
            id = "com.composekn.linux-native-linker"
            implementationClass = "com.composekn.gradle.LinuxNativeLinkerPlugin"
        }
        create("windowsNativeLinker") {
            id = "com.composekn.windows-native-linker"
            implementationClass = "com.composekn.gradle.WindowsNativeLinkerPlugin"
        }
        create("composeKnResources") {
            id = "com.composekn.resources"
            implementationClass = "com.composekn.gradle.ComposeKnResourcesPlugin"
        }
        create("composeKnHost") {
            id = "com.composekn.host"
            implementationClass = "com.composekn.gradle.ComposeKnHostPlugin"
        }
        create("composeKnApp") {
            id = "com.composekn.app"
            implementationClass = "com.composekn.gradle.ComposeKnAppPlugin"
        }
        create("composeKnPublish") {
            id = "com.composekn.publish"
            implementationClass = "com.composekn.gradle.ComposeKnPublishPlugin"
        }
        create("composeKnSettings") {
            id = "com.composekn.settings"
            implementationClass = "com.composekn.gradle.ComposeKnSettingsPlugin"
        }
        // 统一入口：与 settings 同一实现（坐标替换 + Release 缓存）
        create("composeKn") {
            id = "com.composekn"
            implementationClass = "com.composekn.gradle.ComposeKnSettingsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "ComposeKnLocal"
            url = uri(rootProject.layout.projectDirectory.dir("../build/maven-repo"))
        }
        val ghToken = System.getenv("GITHUB_TOKEN")
        val wantGh = providers.gradleProperty("composekn.publish.github").orNull == "true"
        if (wantGh && !ghToken.isNullOrBlank()) {
            val owner = System.getenv("GITHUB_REPOSITORY_OWNER") ?: "TetraploidHuman"
            val repo = System.getenv("GITHUB_REPOSITORY")?.substringAfter('/')
                ?: "compose-multiplatform-kotlin-native"
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/$owner/$repo")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: "github"
                    password = ghToken
                }
            }
        }
    }
}
