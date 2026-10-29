import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The game itself: plain Kotlin and Compose code shared by the Android app and the web app.
// An iOS target can be added to the `kotlin { }` block later (iosArm64, iosSimulatorArm64) without moving any code.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.avabuilds.packetrunner.shared"
        compileSdk = 37
        minSdk = 24

        // run the common tests on the JVM too (./gradlew :shared:testAndroidHostTest)
        withHostTestBuilder {}
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            // the common tests run in headless Chrome (./gradlew :shared:wasmJsBrowserTest); set CHROME_BIN if Chrome is not found.
            // ("no sandbox" is what a CI machine running as root needs; it changes nothing else)
            testTask {
                useKarma { useChromeHeadlessNoSandbox() }
            }
        }
        binaries.executable() // Compose needs this so its graphics runtime is bundled for the tests
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
