import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
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

        testInstrumentationRunner = "com.informatique.mtcit.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_KEY", "\"51563d451c6f724bd8ab8b996d791403\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_KEY", "\"51563d451c6f724bd8ab8b996d791403\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    // --- Compose BOM ---
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // --- Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Compose UI ---
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.coil.compose)

    // --- Accompanist ---
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.navigation.animation)

    // --- Lifecycle ---
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // --- Coroutines ---
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // --- Serialization & Ktor ---
    implementation(libs.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // --- Logging ---
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    // --- Hilt ---
    implementation(libs.hilt.android)
    add("ksp", libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    add("ksp", libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.work)

    // --- Room ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    add("ksp", libs.room.compiler)

    // --- WorkManager ---
    implementation(libs.work.runtime)

    // --- Paging ---
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // --- DataStore ---
    implementation(libs.datastore.preferences)

    // --- Splash Screen ---
    implementation(libs.core.splashscreen)

    // --- Navigation ---
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.navigation.compose)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    // --- Hilt Testing ---
    testImplementation(libs.hilt.android)
    add("kspTest", libs.hilt.compiler)
    androidTestImplementation(libs.hilt.android)
    add("kspAndroidTest", libs.hilt.compiler)

}
