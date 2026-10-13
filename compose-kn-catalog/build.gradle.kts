plugins {
    `version-catalog`
    `maven-publish`
    id("com.tetraploid.publish")
}

group = "com.tetraploid"
version = providers.gradleProperty("composekn.version").orElse("0.5.68").get()

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
