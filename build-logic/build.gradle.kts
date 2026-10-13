plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.tetraploid"
version = providers.gradleProperty("tetraploid.version").orElse("0.5.68").get()

dependencies {
    implementation(gradleApi())
    // Must be implementation (not compileOnly): Maven consumers load host/resources plugins
    // in an isolated classloader; decorating TetraploidHostPlugin needs KMP types on that path.
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
            id = "com.tetraploid.konan-link-workaround"
            implementationClass = "com.tetraploid.gradle.KonanLinkWorkaroundPlugin"
        }
        create("linuxNativeLinker") {
            id = "com.tetraploid.linux-native-linker"
            implementationClass = "com.tetraploid.gradle.LinuxNativeLinkerPlugin"
        }
        create("windowsNativeLinker") {
            id = "com.tetraploid.windows-native-linker"
            implementationClass = "com.tetraploid.gradle.WindowsNativeLinkerPlugin"
        }
        create("tetraploidResources") {
            id = "com.tetraploid.resources"
            implementationClass = "com.tetraploid.gradle.TetraploidResourcesPlugin"
        }
        create("tetraploidHost") {
            id = "com.tetraploid.host"
            implementationClass = "com.tetraploid.gradle.TetraploidHostPlugin"
        }
        create("tetraploidApp") {
            id = "com.tetraploid.app"
            implementationClass = "com.tetraploid.gradle.TetraploidAppPlugin"
        }
        create("tetraploidPublish") {
            id = "com.tetraploid.publish"
            implementationClass = "com.tetraploid.gradle.TetraploidPublishPlugin"
        }
        create("tetraploidSettings") {
            id = "com.tetraploid.settings"
            implementationClass = "com.tetraploid.gradle.TetraploidSettingsPlugin"
        }
        // 统一入口：与 settings 同一实现（坐标替换 + Release 缓存）
        create("tetraploid") {
            id = "com.tetraploid"
            implementationClass = "com.tetraploid.gradle.TetraploidSettingsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "TetraploidLocal"
            url = uri(rootProject.layout.projectDirectory.dir("../build/maven-repo"))
        }
        val ghToken = System.getenv("GITHUB_TOKEN")
        val wantGh = providers.gradleProperty("tetraploid.publish.github").orNull == "true"
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
