import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/*
 * 签名凭据读取。
 *
 * 历史问题：storePassword / keyPassword 曾以明文写死在本文件里，而本文件是被版本库跟踪的，
 * 导致发布签名口令随公开仓库一起泄露（详见 _audit/代码审计报告.md 的 P0-1）。
 *
 * 现在改为按「环境变量 → local.properties」的顺序读取，脚本里不再出现任何口令字面量。
 * local.properties 已被 .gitignore 排除，不会进入版本库；CI 环境用环境变量注入。
 * 若两者都取不到，签名口令为空会让 release 构建直接失败——这是刻意的：宁可构建失败，
 * 也不要退回硬编码。
 */
val signingProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String =
    System.getenv(envName) ?: signingProps.getProperty(propName) ?: ""

android {
    namespace = "com.hdu.cetvocab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hdu.cetvocab"
        minSdk = 26
        targetSdk = 34
        versionCode = 23
        versionName = "1.2.2"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = file("cetvocab-release.jks")
            // 口令一律从环境变量 / local.properties 读取，脚本内不留明文
            storePassword = signingValue("CETVOCAB_STORE_PASSWORD", "cetvocab.storePassword")
            keyAlias = signingValue("CETVOCAB_KEY_ALIAS", "cetvocab.keyAlias")
            keyPassword = signingValue("CETVOCAB_KEY_PASSWORD", "cetvocab.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // 保留 lint 报告，但不用它阻断出包。
        // 历史设置把 checkReleaseBuilds 也关了，等于 release 构建完全不跑 lint ——
        // 报告里的「多余权限」「主题缺 night 限定符」「废弃 API」等本来都是 lint 的默认检查项，
        // 关掉之后就再也没人提示过。现在恢复检查，只把「失败即中断」关掉。
        abortOnError = false
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
