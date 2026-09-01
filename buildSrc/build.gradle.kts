plugins {
    `kotlin-dsl`
    // kotlin("jvm") version "2.1.0"
}

gradlePlugin {
    plugins {
        register("lottiePreParser") {
            id = "org.telegram.lottie-meta"
            implementationClass = "org.telegram.lottie.LottieMetaPlugin"
        }
        register("testGenerator") {
            id = "test-generator"
            implementationClass = "com.example.TestGeneratorPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}
/*
val checkEmojiKeyboard by tasks.registering(GenerateSchemeTask::class) {

}
*/
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
    incremental = false
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.10.1")
    // nicegram: AGP drags in javapoet 1.10.0, and buildSrc's runtime classpath is prepended
    // to the buildscript classloader — that shadows the 1.13.0 Hilt needs for
    // ClassName.canonicalName() and breaks hiltAggregateDeps. Pin the newer one here.
    implementation("com.squareup:javapoet:1.13.0")

    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.github.javaparser:javaparser-core:3.25.4")
    implementation("com.squareup:kotlinpoet:1.15.0")
    implementation("com.google.code.gson:gson:2.11.0")
}