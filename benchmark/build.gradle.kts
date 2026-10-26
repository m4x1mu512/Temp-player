plugins {
  id("com.android.test")
}

android {
  namespace = "com.example.benchmark"
  compileSdk = 36

  defaultConfig {
    minSdk = 26
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":app"
  // Macrobenchmark drives the app through UiAutomator, so it has to instrument itself.
  experimentalProperties["android.experimental.self-instrumenting"] = true

  buildTypes {
    create("benchmark") {
      isDebuggable = true
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
    }
  }
}

dependencies {
  implementation(libs.androidx.benchmark.macro.junit4)
  implementation(libs.androidx.junit)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.runner)
}
