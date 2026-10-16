# 过四斩六：源码与构建说明

> 一款**完全离线**的四六级背单词Android应用。

---

## 一、这是什么

一个可直接安装使用的Android APK（`过四斩六-v1.4.4-离线版.apk`，9.08 MB），内置10998条四六级词条
（每单元50词），提供发音、释义、例句、词根词缀、联想记忆五层内容，并带单词闯关、
「四级/六级全词挑战」（本级随机50题）、易混词辨析与真题句子翻译两类拓展练习、
按首字母的全词预览与中英双向搜索、历次成绩排行榜、81项成就系统、基于SM-2的间隔重复复习、
CSV导出与JSON备份恢复、可切换的桌面图标，以及跟随系统的深色配色。

**关键特征：完全离线。**安装包中不含网络请求代码，AndroidManifest里**未申请任何权限**——
不是"承诺不联网"，而是技术上联不了网。学习数据全部保存在手机本地数据库。

---

## 二、环境要求

| 项 | 版本 |
|---|---|
| JDK | 17（Temurin 17.0.20.1已验证） |
| Gradle | 8.7 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose Compiler | 1.5.14 |
| Android SDK | compileSdk 34 / build-tools 34.0.0 |
| minSdk/targetSdk | 26（Android 8.0）/34 |

> ⚠️ Gradle需要JDK在`PATH`上，仅用`-Dorg.gradle.java.home=`指定是不够的。
> 构建前请先`export PATH="<jdk-17>/bin:$PATH"`。

---

## 三、目录结构

```
CetVocab/
├── settings.gradle.kts           # 仓库源（含阿里云镜像，加速依赖解析）
├── build.gradle.kts              # 顶层构建脚本
├── gradle.properties             # JVM 内存与并行/缓存开关
├── app/
│   ├── build.gradle.kts          # 应用模块：Compose、签名、打包配置
│   ├── proguard-rules.pro        # 保留 com.hdu.cetvocab 包
│   ├── cetvocab-release.jks      # ❌ 未随包分发（签名密钥）
│   └── src/main/
│       ├── AndroidManifest.xml   # 不声明任何权限；无 INTERNET
│       ├── assets/words.db       # 内置只读词库（7.61 MB，10998 词条）
│       ├── res/                  # 主题、配色、自适应图标（界面零位图；
│       │                         #  桌面图标两套简洁字形「四」「六」由系统字体渲染为 PNG 前景）
│       └── java/com/hdu/cetvocab/
│           ├── CetVocabApp.kt
│           ├── MainActivity.kt
│           ├── data/             # Models / WordDb / UserDb / VocabRepository（含 SM-2 调度）/ BackupManager
│           └── ui/               # AppRoot / AppViewModel / theme（AppColors 主题色）/ components / screens
└── tools/                        # 词库构建与校验脚本（Python）
    ├── build_words_db.py         # 七步数据流水线
    ├── verify_db.py              # PC 端 SQL 校验（与 App 同语句）
    ├── explore.py                # 开源数据集探查
    ├── pack_source.py            # 源码打包脚本
    └── data/mnemonics.json       # 586 条人工审核的精选谐音/联想
```

---

## 四、构建步骤

```bash
# 1) 准备 JDK 17 并放进 PATH
export PATH="/path/to/jdk-17/bin:$PATH"

# 2) 在项目根目录写 local.properties：SDK 路径 + 签名三项
#    （该文件已被 .gitignore 排除，口令不会进版本库）
cat > local.properties <<'EOF'
sdk.dir=/path/to/android-sdk
cetvocab.storePassword=<你的 keystore 口令>
cetvocab.keyAlias=<别名>
cetvocab.keyPassword=<私钥口令>
EOF

# 3) 构建 release APK
gradle assembleRelease

# 产物：app/build/outputs/apk/release/app-release.apk
```

### 关于签名（读这段再动手，否则会撞一个「看不懂的构建失败」）

- 仓库**不含**签名密钥：`app/cetvocab-release.jks`与口令都不入库。要出可安装的release包，
需自行把keystore放到`app/cetvocab-release.jks`，并在`local.properties`里给出上面三项。
- 三项也可以不写文件、改用环境变量注入，适合CI：
  `CETVOCAB_STORE_PASSWORD` / `CETVOCAB_KEY_ALIAS` / `CETVOCAB_KEY_PASSWORD`
（优先级：环境变量→local.properties）。
- **两处都取不到时release构建会直接失败**，这是刻意设计：宁可失败，也不退回把口令硬编码进
  `build.gradle.kts`（历史上正发生过，见提交记录与`CHANGELOG.md`）。只想看功能的话跑debug即可：
  `gradle assembleDebug`，无需任何签名配置（debug包applicationId带`.debug`后缀，可与release并存）。
- 要换自己的密钥：`keytool -genkeypair -V3 -keystore cetvocab-release.jks -storetype PKCS12 -alias <别名> -dname "CN=..." -validity 10000 -keyalg RSA -keysize 2048`。
注意**换证书后老用户无法覆盖升级**（签名不同会被系统拒绝），只能卸载重装；本项目历史版本共用同一把证书。
- 历史遗留：早期版本的`build.gradle.kts`里出现过明文口令，且仓库曾公开。签名口令已轮换（证书不变，
覆盖升级仍可用），但公开仓库的历史提交里仍能看到旧字串——它已失效，仅作提示。


---

## 五、重建词库（可选）

内置词库`app/src/main/assets/words.db`由`tools/build_words_db.py`生成，流程为：

1. 以ECDICT开源词典的`cet4` / `cet6`大纲标签确定词表范围，并用两本开源词书补充；
2. 合并ECDICT与`english-vocabulary`开源数据，补齐中文释义、英文释义、词性与英美双音标；
3. 从14万条中英例句中用**打分函数**挑选最佳例句（整词匹配优先，惩罚数字、专有名词、超长句）；
4. 用规则+词典校验自动拆解词根词缀（要求剩余词干是真实单词）；
5. 加载`tools/data/mnemonics.json`中人工审核过的586条精选谐音/联想；
6. 用编辑距离算法计算形近词；
7. 建索引后`VACUUM`压缩，输出SQLite。

```bash
python tools/build_words_db.py
python tools/verify_db.py        # 用与 App 完全相同的 SQL 语句做校验
```

---

## 六、数据来源与授权

| 数据项 | 来源 | 授权 |
|---|---|---|
| 四六级大纲词汇范围 | ECDICT开源词典库的`cet4` / `cet6`标签 | MIT License |
| 补充词表 | 开源词书《四级词汇乱序便携版》《星火四级词汇必背乱序版》 | 开源整理数据 |
| 中文/英文释义、词性、词频、词形变化 | ECDICT（skywind3000/ECDICT） | MIT License |
| 英美双音标、例句语料 | 开源词库english-vocabulary（zhenghaoyang24） | 开源整理数据 |
| 词根词缀拆解、形近词辨析 | 团队自研算法基于上述数据构建 | 团队原创 |
| 谐音/联想记忆内容 | 团队使用AIGC工具批量生成后**人工逐条审核** | 团队原创（AI辅助，已标注） |
| 图标与界面素材 | 团队Canvas绘制+系统矢量图标 | 团队原创，不含第三方图片素材 |

---

## 七、AI工具使用说明

本项目在开发过程中使用了AIGC工具，具体环节已在项目文档中如实说明：

| 环节 | 工具 |
|---|---|
| 需求与产品设计 | 豆包 |
| 界面与业务代码 | Qoder CN |
| 助记内容生成 | 通义千问/DeepSeek |
| 数据处理脚本 | DeepSeek |
| 文档与PPT | 豆包 |
| 测试用例 | DeepSeek |

**所有AI生成内容均经过人工调试、修改与审核**，不存在直接提交未验证产物的情况。
