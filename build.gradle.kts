// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1") // Updated to match plugin version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20") // Updated to match plugin version
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.54") // Updated to match your hiltVersion
    }
}

plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.library") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false // Added apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
    id("com.google.dagger.hilt.android") version "2.54" apply false // Added Hilt plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false // Added Hilt plugin
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}