import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Environment config lives in the git-ignored local.properties (see local.properties.example),
// mirroring the Expo app's .env.dev / .env.release split.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun prop(key: String): String =
    localProps.getProperty(key) ?: error("Missing '$key' in local.properties")

val authScheme = "expensestracker"

// Release signing comes from ~/.gradle/gradle.properties (outside the repo, with the keystore in
// ~/.android/keystores). Without it the release build is left unsigned.
fun signingProp(name: String): String? = providers.gradleProperty("expensesTracker.release.$name").orNull

android {
    namespace = "com.jldev.expensestracker.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jldev.expensestracker.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"

        buildConfigField("String", "AUTH0_AUDIENCE", "\"${prop("auth0.audience")}\"")
        buildConfigField("String", "AUTH0_SCHEME", "\"$authScheme\"")
    }

    signingConfigs {
        create("release") {
            signingProp("storeFile")?.let { storeFile = file(it) }
            storePassword = signingProp("storePassword")
            keyAlias = signingProp("keyAlias")
            keyPassword = signingProp("keyPassword")
        }
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingProp("storeFile") != null) signingConfig = signingConfigs.getByName("release")
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

    testOptions {
        // Robolectric needs Android resources for Room tests on the JVM.
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

// Robolectric (Room tests on the JVM) doesn't support the newest JDKs; run unit tests on JDK 21.
tasks.withType<Test>().configureEach {
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    // Robolectric's file-descriptor shadow reaches into JDK internals.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
}

room {
    // Exported schemas are what future Room migrations are written against.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

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

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
