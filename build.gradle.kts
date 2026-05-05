plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "goit.tracker"
version = "1.6.0"

repositories {
    mavenCentral()
}

intellij {
    type.set("PY")
    version.set("2023.3")
    updateSinceUntilBuild.set(false)
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("")
    }

    prepareSandbox {
        from("src/main/resources/default-config.json") {
            into(project.name)
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
