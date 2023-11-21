import java.util.*

plugins {
    id("common-conventions")
    id("kotlin-conventions")
    application
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.javalin:javalin:6.0.0-beta.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.spongepowered:configurate-jackson:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-hocon:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0-SNAPSHOT")
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }
    mitLicense()
}

application {
    mainClass.set("dev.mizule.imagery.app.launcher.Launcher")
}

tasks {
    clean {
        delete("run")
    }

    runShadow {
        workingDir = file("run").also(File::mkdirs)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
