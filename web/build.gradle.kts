import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The web app: runs the shared game in a browser (Kotlin/Wasm). `./gradlew :web:wasmJsBrowserDevelopmentRun` opens it locally.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "web"
        browser {
            commonWebpackConfig {
                outputFileName = "web.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.avabuilds.packetrunner.web.generated"
    publicResClass = false
    generateResClass = always
}
