plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

room {
    // Room writes a JSON description of every database version here. It is committed to git and
    // lets migration tests compare the old and new schema. See docs/database.md.
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "dev.malkolm.recipeapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.malkolm.recipeapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Any lint warning fails the build, so problems are fixed when they appear instead of piling up.
        warningsAsErrors = true
        // "A newer version is available" reminders are not bugs. Left on, they would turn CI red
        // the day a new Gradle/AGP/library release appears, with no code change. Keeping
        // dependencies current is a separate, deliberate job.
        disable +=
            setOf(
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable"
            )
    }
    testOptions {
        unitTests {
            // Robolectric needs the merged resources/manifest to build a fake Android environment.
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        // Migration tests load the exported Room schemas as assets. Robolectric only sees the
        // debug build's assets (not a separate test asset folder), so they are added there.
        // Release builds do not contain them.
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

ktlint {
    version.set(libs.versions.ktlint)
}

tasks.withType<Test>().configureEach {
    // Robolectric fakes Android by reaching into JDK internals, which Java 17+ hides by default.
    jvmArgs(
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
}
