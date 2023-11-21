import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common-conventions")
    kotlin("jvm")
}

tasks {
    withType(KotlinCompile::class).configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_1_9)
        }
    }
}

extensions.configure(KotlinProjectExtension::class) {
    jvmToolchain(17)
}