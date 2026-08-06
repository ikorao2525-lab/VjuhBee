plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vjuhbee.beecalc"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vjuhbee.beecalc"
        minSdk = 28
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Три канала публикации (SPEC.md §4, §9):
    // play    — Google Play, без кнопки доната
    // rustore — RuStore, донат «чашка кофе»
    // beta    — отдельный applicationId, тестовый премиум, бета-фичи
    flavorDimensions += "store"
    productFlavors {
        create("play") {
            dimension = "store"
            buildConfigField("String", "STORE_CHANNEL", "\"play\"")
            buildConfigField("boolean", "SHOW_DONATE", "false")
            buildConfigField("boolean", "IS_BETA", "false")
        }
        create("rustore") {
            dimension = "store"
            buildConfigField("String", "STORE_CHANNEL", "\"rustore\"")
            buildConfigField("boolean", "SHOW_DONATE", "true")
            buildConfigField("boolean", "IS_BETA", "false")
        }
        create("beta") {
            dimension = "store"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            buildConfigField("String", "STORE_CHANNEL", "\"beta\"")
            buildConfigField("boolean", "SHOW_DONATE", "true")
            buildConfigField("boolean", "IS_BETA", "true")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
