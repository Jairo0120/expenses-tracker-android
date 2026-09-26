import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Environment config lives in the git-ignored local.properties (see local.properties.example),
// mirroring the Expo app's .env.dev / .env.release split.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun prop(key: String): String =
    localProps.getProperty(key) ?: error("Missing '$key' in local.properties")

val authScheme = "expensestracker"

android {
    namespace = "com.jldev.expensestracker.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jldev.expensestracker.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "AUTH0_AUDIENCE", "\"${prop("auth0.audience")}\"")
        buildConfigField("String", "AUTH0_SCHEME", "\"$authScheme\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "(DEV) Expenses Tracker")
            buildConfigField("String", "API_URL", "\"${prop("dev.apiUrl")}\"")
            buildConfigField("String", "AUTH0_DOMAIN", "\"${prop("dev.auth0.domain")}\"")
            buildConfigField("String", "AUTH0_CLIENT_ID", "\"${prop("dev.auth0.clientId")}\"")
            manifestPlaceholders += mapOf(
                "auth0Domain" to prop("dev.auth0.domain"),
                "auth0Scheme" to authScheme,
                // The dev API runs over plain http on the LAN.
                "usesCleartextTraffic" to "true",
            )
        }
        release {
            isMinifyEnabled = false
            resValue("string", "app_name", "Expenses Tracker")
            buildConfigField("String", "API_URL", "\"${prop("release.apiUrl")}\"")
            buildConfigField("String", "AUTH0_DOMAIN", "\"${prop("release.auth0.domain")}\"")
            buildConfigField("String", "AUTH0_CLIENT_ID", "\"${prop("release.auth0.clientId")}\"")
            manifestPlaceholders += mapOf(
                "auth0Domain" to prop("release.auth0.domain"),
                "auth0Scheme" to authScheme,
                "usesCleartextTraffic" to "false",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.auth0)
}
