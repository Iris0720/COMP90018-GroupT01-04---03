import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Load local.properties BEFORE android {}
val props = Properties().apply {
    rootProject.file("local.properties").inputStream().use {
        load(it)
    }
}

android {
    namespace = "com.example.comp90018"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.comp90018"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${props.getProperty("SUPABASE_URL")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${props.getProperty("SUPABASE_PUBLISHABLE_KEY")}\""
        )
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

    dependencies {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.tooling.preview)
        debugImplementation(libs.androidx.compose.ui.tooling)

        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.core.ktx)
        implementation(libs.material)

        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

        implementation("androidx.navigation:navigation-compose:2.8.9")

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

        implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
        implementation("io.github.jan-tennert.supabase:auth-kt")
        implementation("io.github.jan-tennert.supabase:postgrest-kt")

        implementation("io.ktor:ktor-client-android:3.5.1")

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.junit)
    }
}