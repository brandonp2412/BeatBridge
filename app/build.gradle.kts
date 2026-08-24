import java.util.Properties

plugins {
    id("com.android.application")
}

val versionProps = Properties().also { props ->
    val f = rootProject.file("version.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.beatbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beatbridge"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionProps["versionCode"] as String?)?.toInt() ?: 1
        versionName = (versionProps["versionName"] as String?) ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Lets emulator-based CI exercise the UI without system permission dialogs.
        testInstrumentationRunnerArguments["grantAll"] = "true"
    }

    val keyPropsFile = sequenceOf(
        rootProject.file("key.properties"),
        File(System.getProperty("user.home"), "flexify/android/key.properties")
    ).firstOrNull { it.exists() }
    val keyProps = Properties().also { props ->
        if (keyPropsFile != null) keyPropsFile.inputStream().use { props.load(it) }
    }

    fun cred(prop: String, env: String) = keyProps.getProperty(prop) ?: System.getenv(env)

    val storePassword = cred("storePassword", "KEYSTORE_PASSWORD")
    if (storePassword != null) {
        signingConfigs {
            create("release") {
                storeFile = cred("storeFile", "KEYSTORE_FILE")
                    ?.let { path ->
                        val f = File(path)
                        if (f.isAbsolute || keyPropsFile == null) f
                        else sequenceOf(
                            File(keyPropsFile.parentFile, path),
                            File(keyPropsFile.parentFile, "app/$path")
                        ).firstOrNull { it.exists() } ?: File(keyPropsFile.parentFile, path)
                    }
                    ?: file("keystore.jks")
                this.storePassword = storePassword
                keyAlias = cred("keyAlias", "KEY_ALIAS")
                keyPassword = cred("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (storePassword != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (storePassword != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = false
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// ABI-specific versionCodes: base * 100 + suffix (x86_64=1, armeabi-v7a=2, arm64-v8a=3, x86=4)
// This matches the VercodeOperation in fdroiddata so F-Droid serves the right APK per device.
private val abiVersionCodes = mapOf("x86_64" to 1, "armeabi-v7a" to 2, "arm64-v8a" to 3, "x86" to 4)

androidComponents {
    onVariants { variant ->
        val base = android.defaultConfig.versionCode ?: 1
        variant.outputs.forEach { output ->
            val abi = output.filters.find {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier
            val suffix = abiVersionCodes[abi] ?: return@forEach
            output.versionCode.set(base * 100 + suffix)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
