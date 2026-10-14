plugins {
    `java-platform`
    id("cn.tetraploid.publish")
}

group = "cn.tetraploid"
version = providers.gradleProperty("composekn.version").getOrElse("0.5.69")

val uiBase = providers.gradleProperty("composekn.compose.ui").orElse("1.12.1").get()
val uiVersion = providers.gradleProperty("composekn.compose.ui.version")
    .orElse("$uiBase-ckn.$version")
    .get()

dependencies {
    constraints {
        // 宿主 / 资源 / Skiko：与 composekn.version 同号
        api("cn.tetraploid:compose-kn-linux:$version")
        api("cn.tetraploid:compose-kn-windows:$version")
        api("cn.tetraploid:compose-kn-resources:$version")
        api("cn.tetraploid:skiko:$version")
        api("cn.tetraploid:composekn-catalog:$version")

        // UI 栈：发布坐标为 1.12.1-ckn.<composekn.version>，消费方只锁 BOM / composekn.version
        listOf(
            "ui", "ui-util", "ui-geometry", "ui-unit", "ui-graphics", "ui-text",
            "ui-backhandler",
            "foundation", "foundation-layout",
            "animation", "animation-core",
            "material-ripple", "material3",
            "lifecycle-viewmodel-compose",
        ).forEach { artifact ->
            api("cn.tetraploid.compose:$artifact:$uiVersion")
        }
    }
}
