@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "it.gmmz.llamandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.gmmz.llamandroid"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a")
                arguments += "-DLLAMA_VERBOSE=ON"
                arguments += "-DLLAMA_CURL=OFF"
                arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
                arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
                arguments += "-DGGML_OPENCL=ON"
//                cppFlags += ""
//                arguments += ""
            }
        }
    }

    val jllamaLib = file("java-llama.cpp")

    // Execute "mvn compile" if folder target/ doesn't exist at ./java-llama.cpp/
    if (!file("$jllamaLib/target").exists()) {
        exec {
            commandLine = listOf("mvn", "compile")
            workingDir = jllamaLib
        }
    }

    // Declare c++ sources
    externalNativeBuild {
        cmake {
            path = file("$jllamaLib/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Declare java sources
    sourceSets {
        named("main") {
            // Add source directory for java-llama.cpp
            java.srcDir("$jllamaLib/src/main/java")
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.github.tonyofrancis.Fetch:fetch2:3.4.1")
    implementation("com.github.tonyofrancis.Fetch:fetch2okhttp:3.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("com.github.jeziellago:compose-markdown:0.5.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("ru.noties:jlatexmath-android:0.2.0")
    implementation("ru.noties:jlatexmath-android-font-cyrillic:0.2.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}