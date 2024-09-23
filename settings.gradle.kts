pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Imagery"

sequenceOf(
    "app",
).forEach {
    include("imagery-$it")
    project(":imagery-$it").projectDir = file(it)
}