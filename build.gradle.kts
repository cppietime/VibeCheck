plugins {
    kotlin("jvm") version "2.0.21" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
