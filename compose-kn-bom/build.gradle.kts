plugins {
    `java-platform`
    id("com.tetraploid.publish")
}

group = "com.tetraploid"
version = providers.gradleProperty("tetraploid.version").getOrElse("0.5.68")

val uiBase = providers.gradleProperty("tetraploid.compose.ui").orElse("1.12.1").get()
val uiVersion = providers.gradleProperty("tetraploid.compose.ui.version")
    .orElse("$uiBase-tp.$version")
    .get()

dependencies {
    constraints {
        // 宿主 / 资源 / Skiko：与 tetraploid.version 同号
        api("com.tetraploid:compose-kn-linux:$version")
        api("com.tetraploid:compose-kn-windows:$version")
        api("com.tetraploid:compose-kn-resources:$version")
        api("com.tetraploid:skiko:$version")
        api("com.tetraploid:tetraploid-catalog:$version")

        // UI 栈：发布坐标为 1.12.1-tp.<tetraploid.version>，消费方只锁 BOM / tetraploid.version
        listOf(
            "ui", "ui-util", "ui-geometry", "ui-unit", "ui-graphics", "ui-text",
            "ui-backhandler",
            "foundation", "foundation-layout",
            "animation", "animation-core",
            "material-ripple", "material3",
            "lifecycle-viewmodel-compose",
        ).forEach { artifact ->
            api("com.tetraploid.compose:$artifact:$uiVersion")
        }
    }
}
