plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.kyori.indra") version "3.1.3"
    application
}

repositories {
    mavenCentral()
    sonatype.s01Snapshots()
    sonatype.ossSnapshots()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.javalin:javalin:6.0.0-beta.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.spongepowered:configurate-gson:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0-SNAPSHOT")
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }
}

application {
    mainClass.set("dev.mizule.imagery.app.launcher.Launcher")
}

tasks {
    clean {
        delete("run")
    }

    runShadow {
        file("run").mkdirs()
        workingDir = file("run")
        systemProperty("terminal.jline", false)
        systemProperty("terminal.ansi", true)
    }
}