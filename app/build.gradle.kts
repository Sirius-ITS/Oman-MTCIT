plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.dagger)
}

android {
    namespace = "com.informatique.mtcit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.informatique.mtcit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            optIn.add("kotlin.RequiresOptIn")
            freeCompilerArgs.addAll(listOf("-Xcontext-parameters", "-Xinline-classes"))
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    coreLibraryDesugaring(libs.android.desugar.sdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)

    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.constrainlayout)
    implementation(libs.androidx.navigation)

    implementation(libs.androidx.viewmodel.ktx)
    implementation(libs.androidx.viewmodel.livedata.ktx)
    implementation(libs.androidx.viewmodel.savedstate)
    implementation(libs.androidx.viewmodel)
    implementation(libs.androidx.runtime)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.kotlin.metadata.jvm)

    implementation(libs.hilt.dagger.android)
    implementation(libs.hilt.dagger.navigation)
    implementation(libs.hilt.dagger.work)
    kapt(libs.hilt.dagger.compiler)

    implementation(libs.ktor.core)
    implementation(libs.ktor.client)
    implementation(libs.ktor.client.cn)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.json)
    implementation(libs.kotlinx.json)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.coil)
    implementation(libs.coil.network)

    implementation(libs.google.ui.controller)
    implementation(libs.google.navigation.animation)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}