plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "cn.tetraploid"
version = providers.gradleProperty("composekn.version").orElse("0.5.71").get()

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
            id = "cn.tetraploid.konan-link-workaround"
            implementationClass = "cn.tetraploid.gradle.KonanLinkWorkaroundPlugin"
        }
        create("linuxNativeLinker") {
            id = "cn.tetraploid.linux-native-linker"
            implementationClass = "cn.tetraploid.gradle.LinuxNativeLinkerPlugin"
        }
        create("windowsNativeLinker") {
            id = "cn.tetraploid.windows-native-linker"
            implementationClass = "cn.tetraploid.gradle.WindowsNativeLinkerPlugin"
        }
        create("composeKnResources") {
            id = "cn.tetraploid.resources"
            implementationClass = "cn.tetraploid.gradle.ComposeKnResourcesPlugin"
        }
        create("composeKnHost") {
            id = "cn.tetraploid.host"
            implementationClass = "cn.tetraploid.gradle.ComposeKnHostPlugin"
        }
        create("composeKnApp") {
            id = "cn.tetraploid.app"
            implementationClass = "cn.tetraploid.gradle.ComposeKnAppPlugin"
        }
        create("composeKnPublish") {
            id = "cn.tetraploid.publish"
            implementationClass = "cn.tetraploid.gradle.ComposeKnPublishPlugin"
        }
        create("composeKnSettings") {
            id = "cn.tetraploid.settings"
            implementationClass = "cn.tetraploid.gradle.ComposeKnSettingsPlugin"
        }
        // 统一入口：与 settings 同一实现（坐标替换 + Release 缓存）
        create("composeKn") {
            id = "cn.tetraploid"
            implementationClass = "cn.tetraploid.gradle.ComposeKnSettingsPlugin"
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
