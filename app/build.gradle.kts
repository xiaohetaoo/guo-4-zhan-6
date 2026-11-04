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
        // 修订号满 10 进位：1.3.9 的这一版是全站文案修订（修订 +1 → 1.3.10），
        // 按项目版本规则写成 v1.4.0。
        // v1.4.1 = 全量代码审查后的缺陷修订（冷启动占位、学习页重复压栈、
        // 搜索截断提示被吞、拓展练习退出落点、错误文案判定等），修订 +1。
        // v1.4.2 = 对 v1.4.1 那批改动本身再审一遍的结果：加载遮罩吞掉手势
        // （原先可穿透盲点底下页面）+ 首页空队列分支在每日目标被篡改为 0 时断言失实。
        // v1.4.3 = 闯关错题重做 + 答题音效 + 桌面图标所见即所得 + 第三轮审查修复。
        // v1.4.4 = 第四轮审查修复：备份恢复的孤儿数据保护、闯关结算前等逐题入库落库、
        // 「我的/设置」显示旧级别旧目标、恢复失败后残留的死按钮、小词量字母恢复必然失败、
        // 音效线程回调竞态与音轨泄漏等。
        // v1.4.6 = 第五轮全量审查的 31 项修复（P1×3 / P2×15 / P3×13）：
        //   旧局快照幂等（K9 回归）、清空数据的并发闸门与异常保护、
        //   用户记录库损坏降级（与词库侧对称）、恢复失败改走全局提示、
        //   app_meta 脏数据不再废掉整次恢复、拼写题空输入守卫、
        //   孤儿错题不再占用重做名额、浅色主题金属色与熟练度圆点对比度、
        //   K13 遗漏的 4 处触摸目标、导出可分享（FileProvider）、
        //   CSV 加 UTF-8 BOM、默认图标补 monochrome、dataExtractionRules 等。
        // v1.4.7 = 两处体验补齐：
        //   ① 四六级切换做出「切换感」—— 指示器改为可过冲的弹簧（spatialPop，
        //      阻尼比 0.62）+ 途中横向拉伸、两级文字反向缩放，
        //      首页整页从「竖向上滑 28dp」改为按切换方向横向推移 24% 屏宽；
        //   ② 新增新手教程 —— 第一次点「开始今日学习」时在学习页上
        //      聚光灯分步高亮五步（进度、卡片、发音、收藏、底部按钮组），
        //      可跳过（跳过也算已看），设置页新增「重新查看新手教程」。
        // v1.4.8 = 第六轮全量审查的 5 项修复（P2×4 / P3×1），全部在真机上复现并验证：
        //   ① 教程遮罩把洞内点击也吞了（文案承诺的操作全部失效）—— 改为遮罩只画不拦、
        //      洞外由四块挡板拦截，洞里的按钮真的能点；
        //   ② 高亮框被父布局撑成竖长条（78×198dp）—— 锚点外层给确定的 size；
        //   ③ 气泡压住被讲解的内容 —— 按实际剩余空间选边 + 第 2 步缩小高亮到「单词+音标」；
        //   ④ userDataProblem 写了但全仓库零调用，用户库损坏时那句话从没出现 —— 接入三条刷新链路；
        //   ⑤ 弹簧过冲被用在缩放上导致「字抖一下」—— 缩放换回临界阻尼。
        // v1.4.9 = 桌面图标切换的两处修复：
        //   ① isAvailable() 用 getComponentEnabledSetting() 判组件是否存在，
        //      而这个 API 对不存在的组件照样返回 DEFAULT、从不抛异常，于是
        //      「点击前先探一下」永远探测不到，设置页在拿不到入口的设备上
        //      照旧承诺「回到桌面即可看到」—— 改查包信息的组件清单；
        //   ② 入口不存在时 setComponentEnabledSetting() 不报错、只写一条偏好，
        //      而自检读的正是这条偏好，于是写入与自检一起「成功」，apply() 返回
        //      APPLIED 但桌面毫无变化 —— 在写入前先判存在性，返回 DISABLED，
        //      并与 FAILED 分开播报（前者重试无意义，后者是这次没写进去）。
        //   另：词库 canonicalIds 的写入锁与 openLock 拆开，首次去重映射不再
        //   排在一次词库打开（损坏自愈时可达数百毫秒）后面。
        // v1.4.10 = 两个新功能（修订号满 10 未进位，按项目规则继续累加）：
        //   ① 学习页「返回上一个词」—— 界面退一格 + 库里那次作答整体撤销
        //      （word_state 逐字段还原 + 删掉 study_log 那一条），
        //      否则同一个词被答两次会让累计学习、正确率、连续打卡虚高；
        //   ② 设置页「一键把高中词汇标为已学会」—— 口径为 ECDICT 的 gk 标签
        //      （官方高考大纲 3677 词），词库交集 2604 词，标为「熟悉(level 3)」
        //      而不是「精通」：既进入「熟悉以上」统计，又仍按间隔复习。
        //   词库随之新增 is_hs 列，WordDb.DB_VERSION 6 → 7（否则老用户拿不到新列）。
        // v1.4.11 = 两处默认值与布局调整：
        //   ① 朗读语速默认值 0.92x → 1.00x（TTS 各厂商引擎的标准语速）。
        //      只改默认值：getFloat 的兜底只在键不存在时生效，
        //      已经手动调过语速的老用户保持自己的值；
        //   ② 设置页「新手教程」卡片从「学习计划」之后移到「应用图标」之后 ——
        //      教程是一次性看过的引导，不该占着第二屏的位置。
        versionCode = 52
        versionName = "1.4.11"
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
