plugins {
    `version-catalog`
    `maven-publish`
    id("cn.tetraploid.publish")
}

group = "cn.tetraploid"
version = providers.gradleProperty("composekn.version").orElse("0.5.69").get()

catalog {
    versionCatalog {
        from(files("../gradle/composekn.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
            artifactId = "composekn-catalog"
        }
    }
}
