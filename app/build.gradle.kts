import java.util.concurrent.TimeUnit

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ===== versionCode 自动化（2026-10-16，优化报告 Phase1-①）=====
// 取 git 提交数作为 versionCode：随提交单调递增，终结「103 个版本 versionCode 恒为 1」的历史。
// git 不可用（未装 git / 无 .git 的构建环境）时回退到固定值，保证构建永不因此失败。
// 回退值取当前提交数：今后 git 计数只会更大；若日后重开新历史，需要手动抬高此值。
val fallbackVersionCode = 91
val gitCommitCount: Int = run {
    try {
        // ProcessBuilder 直调，不依赖 Gradle providers.exec 的 API 版本差异
        val proc = ProcessBuilder("git", "-C", rootDir.absolutePath, "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor(10, TimeUnit.SECONDS)
        out.toIntOrNull() ?: fallbackVersionCode
    } catch (_: Throwable) {
        fallbackVersionCode
    }
}

android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    buildFeatures {
        compose = true
    }

    namespace = "com.example.s1client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.s1client"
        minSdk = 24
        targetSdk = 34
        // versionCode = git 提交数（见文件顶部）；versionName 升到 1.1.0：Beta 阶段起点
        versionCode = gitCommitCount
        versionName = "1.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    buildFeatures {
        buildConfig = true

        viewBinding = true
    }

    // 签名口令不再明文写在脚本里：从 local.properties 读（该文件不入库）。
    // 缺失时 release 构建会明确报错，而不是悄悄用弱口令打包。
    val signingStore = file(
        (project.findProperty("s1.storeFile") as String?) ?: "s1client.jks"
    )
    val signingStorePassword = (project.findProperty("s1.storePassword") as String?)
        ?: ""
    val signingKeyPassword = (project.findProperty("s1.keyPassword") as String?)
        ?: signingStorePassword
    val signingKeyAlias = (project.findProperty("s1.keyAlias") as String?)
        ?: "s1client"

    signingConfigs {
        if (signingStorePassword.isNotBlank() && signingStore.exists()) {
            create("release") {
                storeFile = signingStore
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    // 原本这里有两个 buildTypes 块（26 行与 60 行），后者语义上覆盖前者，容易误读。
    // 现在只保留一个；release 走混淆 + 资源压缩。
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // lifecycle-runtime-ktx 原本声明了两次（2.6.2 与 2.7.0），2026-10-16 去重保留高版本

    implementation("androidx.compose.foundation:foundation")

    // 自动添加的 Compose 依赖
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")

    // 单元测试（JVM，不进 APK）
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Roborazzi 截图测试：CI Linux 上以 NATIVE 图形冒烟截图落盘（Windows 本机 NATIVE 起不来，CiSmokeCaptureTest 首行 Assume 跳过）。
    // 1.8.0 与既有 Robolectric 4.11.1 + Compose BOM 2024.02.00 同时代可用；1.8 线不存在 alpha-13（仅 alpha-1..6 / rc-1..2 / 正式版 1.8.0）。
    // 用 onRoot().captureRoboImage 手法，无需 JUnit rule，故不引 roborazzi-junit-rule（roborazzi-junit4 工件在 Maven Central 不存在）。
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.8.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.8.0")
}
