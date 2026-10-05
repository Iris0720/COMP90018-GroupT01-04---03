import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}
val localPropertiesFile = rootProject.file("local.properties")

val props = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")

    require(localPropertiesFile.exists()) {
        "local.properties not found at: ${localPropertiesFile.absolutePath}"
    }

    localPropertiesFile.inputStream().use {
        load(it)
    }
}

println("LOCAL PROPERTIES PATH = ${localPropertiesFile.absolutePath}")
println("LOCAL PROPERTIES EXISTS = ${localPropertiesFile.exists()}")
println("LOCAL PROPERTIES KEYS = ${props.stringPropertyNames()}")


val supabaseUrl = requireNotNull(props.getProperty("SUPABASE_URL")) {
    "SUPABASE_URL is missing from local.properties"
}

val supabaseKey = requireNotNull(props.getProperty("SUPABASE_PUBLISHABLE_KEY")) {
    "SUPABASE_PUBLISHABLE_KEY is missing from local.properties"
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
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"$supabaseKey\""
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
}

// IMPORTANT: dependencies is OUTSIDE android {}
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