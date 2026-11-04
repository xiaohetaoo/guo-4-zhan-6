package com.hdu.cetvocab.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdu.cetvocab.R
import com.hdu.cetvocab.data.VocabRepository
import com.hdu.cetvocab.data.WordDb
import com.hdu.cetvocab.ui.AppViewModel
import com.hdu.cetvocab.ui.components.AppCard
import com.hdu.cetvocab.ui.components.AppTopBar
import com.hdu.cetvocab.ui.components.BulletRow
import com.hdu.cetvocab.ui.components.IconChevronRight
import com.hdu.cetvocab.ui.components.MenuRow
import com.hdu.cetvocab.ui.components.SectionTitle
import com.hdu.cetvocab.util.LauncherIcons
import java.io.File
import java.util.Locale
import com.hdu.cetvocab.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onReplayTutorial: () -> Unit) {
    val state by vm.profile.collectAsState()
    var showReset by remember { mutableStateOf(false) }

    // 每日目标滑杆：拖动过程中只更新本地值，松手才落盘，
    // 避免拖动时每帧都写偏好并触发整页刷新。
    var goalSlider by remember(state.goal) { mutableFloatStateOf(state.goal.toFloat()) }
    // 语速滑杆同理：此前是每帧调 vm.setSpeechRate()，等于每帧写一次盘 + 刷新整页。
    var rateSlider by remember(state.speechRate) { mutableFloatStateOf(state.speechRate) }

    val context = LocalContext.current

    // 导出 / 备份 / 恢复的结果由 ViewModel 统一持有：
    // 这些是文件 IO，不能在页面作用域里跑（用户一离开页面协程就被取消，
    // 会留下写了一半的文件）。
    // 结果由 ViewModel 持有，因此用户点完「导出」立刻返回上一页时任务照跑，
    // 完成后写入的结果却没人消费 —— 于是下次（可能是几天后）进设置页会凭空
    // 弹出一条过期成功框。进页面时先把上一次的遗留结果丢掉：文件本身已经落盘，
    // 真要路径再导一次即可，一条几小时前的成功提示没有参考价值。
    //
    // 清空必须发生在 collectAsState **之前**，所以放在 remember 的初始化块里而不是
    // 页尾的 LaunchedEffect：collectAsState 拿 StateFlow 的当前值当组合初值，
    // LaunchedEffect 要等这一帧组合完才跑，于是过期成功框会先显示一帧再消失。
    //
    // wordbookMismatch 也必须在这里一并丢掉：它**不会**被任何 consume 自动清空，
    // 而恢复失败/不一致的结果现在走全局 notify（见 AppViewModel.restoreBackup），
    // 页面级这个弹框若留着，用户几天后再进设置页会凭空看到「这份备份来自另一版词库」。
    //
    // 注意必须**有非 Unit 的返回值**：`remember { … }` 返回 Unit 会被 lint 判成错误
    // （RememberReturnType）—— remember 在组合期执行，只在里面做副作用而不产出值，
    // 正是该规则要拦的写法。这里借返回值把三次 consume 串起来，
    // 值本身不参与后续逻辑（真正要用的是下面两个 collectAsState）。
    remember {
        vm.consumeFileDone()
        vm.consumeFileError() to vm.dismissWordbookMismatch()
    }
    val fileDone by vm.fileDone.collectAsState()
    val fileError by vm.fileError.collectAsState()
    // 有文件任务在跑时把四行禁用：连点会并发写盘，备份名只到秒，
    // 同一秒内的两次会算出同一个路径，交错写盘能留下一个半截 JSON
    val fileBusy by vm.fileBusy.collectAsState()

    // 恢复流程状态（纯 UI 选择，不需要跨页面存活）
    var restoreChoices by remember { mutableStateOf<List<File>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<File?>(null) }
    // 备份与当前词库不是同一版时，由数据层抛出、这里负责问用户一句
    val wordbookMismatch by vm.wordbookMismatch.collectAsState()

    /** 复制路径。返回是否真的复制成功 —— 取不到剪贴板服务时要让调用方说一句 */
    fun copyPath(path: String): Boolean {
        // getSystemService 的返回值声明为 @Nullable，个别 ROM 上剪贴板服务会取不到；
        // 直接 as ClipboardManager 就是 NPE 崩溃，这里安全转换并如实返回失败。
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        return runCatching {
            cm.setPrimaryClip(ClipData.newPlainText("path", path))
        }.isSuccess
    }

    fun pickRestore() {
        vm.loadBackups { files ->
            if (files.size == 1) restoreTarget = files.first() else restoreChoices = files
        }
    }

    /*
     * 高中课标词的数量。进页面读一次即可（词库是只读的，这个数不会变）。
     *
     * 必须放到 IO 线程：这里可能在冷启动第一次进设置页时触发词库首次打开，
     * 而「首次打开」包含把 7.98 MB 词库从 assets 拷到私有目录，
     * 在主线程做会肉眼可见地卡一下。初始值 0 只是占位，
     * 读到真值前副标题不提数字，不会说出一个错的数。
     */
    var hsCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        hsCount = vm.highSchoolWordCount()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "设置", onBack = onBack)

        LazyColumn(
            // top 与首页 / 「我的」保持一致（都是 14dp）：此前缺了 top，
            // 首卡紧贴顶栏，切页时肉眼可见地「跳一下」。
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- 学习计划 ----------
            item {
                AppCard {
                    SectionTitle(text = "学习计划")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "每日学习目标",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "到期复习与新词一起计入今日学习量",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${goalSlider.roundToInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "词/天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value = goalSlider,
                        onValueChange = { goalSlider = it },
                        onValueChangeFinished = { vm.setDailyGoal(goalSlider.roundToInt()) },
                        valueRange = 5f..100f,
                        /*
                         * Compose Slider 的 `steps` 是「两端之间的内部档数」：
                         * 可选值个数 = steps + 2，相邻档位间隔 = (end - start) / (steps + 1)。
                         * 所以 5..100（共 95 词）要「一格一词」得写 94：95 / (94 + 1) = 1。
                         *
                         * 此前写的是 18，间隔 = 95 / 19 = 5 —— 落点是 5、10、15…100，
                         * 每一格能选、不会跳，但只能按 5 递增：想把每日目标调成 23 词都做不到，
                         * 而学习页/首页的目标口径是任意整数。这里改成 94 是为了让滑杆的
                         * 可选值与那个口径对齐，不是修「选不出默认值」那样的坏档。
                         */
                        steps = 94,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------- 新手教程 ----------
            item {
                AppCard {
                    SectionTitle(text = "新手教程")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "第一次点「开始今日学习」时会自动展示一次，告诉你这一屏每个地方是做什么用的。" +
                            "已经看过的话，可以在这里重新走一遍。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuRow(
                        title = "重新查看新手教程",
                        subtitle = "装一局今日学习，把五个步骤再讲一遍",
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = { onReplayTutorial() }
                    )
                }
            }

            // ---------- 高中词汇 ----------
            item {
                AppCard {
                    SectionTitle(text = "高中词汇")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (hsCount > 0)
                            "按官方课标（高考大纲）的词汇表，本词库里共有 $hsCount 个词属于高中范围。" +
                                "如果这些词你早就背熟了，可以一次性标为已学会，" +
                                "每日学习队列就会让位给还没掌握的四六级词。"
                        else
                            "按官方课标（高考大纲）的词汇表，本词库里的高中范围词汇可以一次性标为已学会，" +
                                "每日学习队列就会让位给还没掌握的四六级词。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuRow(
                        title = "把高中词汇标为已学会",
                        subtitle = if (fileBusy) "正在处理…" else "标记为「熟悉」，仍会按间隔复习；可随时在词库页逐个调整",
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = { vm.markHighSchoolLearned() }
                    )
                }
            }

            // ---------- 外观 ----------
            item {
                AppCard {
                    SectionTitle(text = "外观")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "明暗模式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "默认跟随系统；也可以手动固定为浅色或深色，不再受系统深浅色开关影响。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            VocabRepository.THEME_SYSTEM to "跟随系统",
                            VocabRepository.THEME_LIGHT to "浅色",
                            VocabRepository.THEME_DARK to "深色"
                        ).forEach { (mode, label) ->
                            val selected = state.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { vm.setThemeMode(mode) }
                                    )
                                    // 芯片高度从 36dp（labelMedium 行高 + 10dp×2）补到 48dp：
                                    // 它是 K13 同批漏掉的一处，触摸目标底线是 48dp。
                                    // 多出的高度由 sizeIn 撑起、文字仍居中，横向版式不变。
                                    .sizeIn(minHeight = 48.dp)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "界面动效",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "关闭后页面切换、展开收起与按压反馈直接落位，不再播放过渡动画",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.animationsEnabled,
                            onCheckedChange = { vm.setAnimationsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "答题音效",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "闯关答对答错时各给一声提示音，答对明亮、答错低沉。" +
                                    "音量跟随手机媒体音量，静音时不响。音效由应用实时合成，不占用安装包体积",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.soundEnabled,
                            // 关掉立刻生效，不用等下一局：正在答题的人按下去就该听不见下一声
                            onCheckedChange = { vm.setSoundEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // ---------- 发音 ----------
            item {
                AppCard {
                    SectionTitle(text = "发音（系统离线语音）")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动朗读",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                // 说清两处触发点：这个开关同时控制学习卡片和闯关拼写题
                                // （QuizScreen 出题时走同一个 speakAuto 入口）。
                                // 原先只写「单词卡片」，用户为了安静复习关掉它，
                                // 会发现拼写题也跟着不出声。
                                text = "学习卡片与拼写题出现时自动播放读音",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoSpeak,
                            onCheckedChange = { vm.setAutoSpeak(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "语速",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(46.dp)
                        )
                        Slider(
                            value = rateSlider,
                            onValueChange = { rateSlider = it },
                            onValueChangeFinished = { vm.setSpeechRate(rateSlider) },
                            valueRange = 0.5f..1.4f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            // 必须显式指定 Locale.ROOT：不传时按设备语言取分隔符，
                            // 德语/法语等环境下会显示成「0,92x」
                            text = String.format(Locale.ROOT, "%.2fx", rateSlider),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "读音由手机系统自带的TTS引擎合成，可离线使用，不消耗流量。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- 应用图标 ----------
            item {
                AppCard {
                    SectionTitle(text = "应用图标")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "桌面图标样式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "简洁图标是橙底白字的大字标，会自动跟随当前级别：四级显示「四」，六级显示「六」。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // 两个预览选项里的字是「选了之后会变成什么」，所以读用户当前级别；
                    // 而下面那句「当前桌面显示」讲的是**实际生效**的图标，字必须来自
                    // 真实 alias（见 desktopStage）—— 两者刻意用不同的来源。
                    val optionChar = if (state.stage == WordDb.STAGE_CET6) "六" else "四"
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconOption(
                            modifier = Modifier.weight(1f),
                            selected = state.iconStyle != LauncherIcons.STYLE_SIMPLE,
                            title = "默认图标",
                            caption = "书本+火花",
                            onClick = { vm.setIconStyle(LauncherIcons.STYLE_DEFAULT) }
                        ) {
                            DefaultIconPreview()
                        }
                        IconOption(
                            modifier = Modifier.weight(1f),
                            selected = state.iconStyle == LauncherIcons.STYLE_SIMPLE,
                            title = "简洁图标",
                            caption = "橙底白字",
                            onClick = { vm.setIconStyle(LauncherIcons.STYLE_SIMPLE) }
                        ) {
                            SimpleIconPreview(
                                if (state.stage == WordDb.STAGE_CET6) R.mipmap.ic_fg_liu
                                else R.mipmap.ic_fg_si
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    /*
                     * 「当前桌面显示」讲系统里真正生效的那个图标，不是用户刚点的选项：
                     * 图标要到 onStop 才落地、个别设备的桌面根本不允许应用换图标，
                     * 之前这句直接按 state.iconStyle 写，切换失败时它一直是假的。
                     * 读不到实际状态（null）时退回按选择显示，不猜。
                     */
                    val desktop = state.desktopIconStyle ?: state.iconStyle
                    val pending = desktop != state.iconStyle
                    /*
                     * 「简洁图标「四/六」」里的字必须来自**实际生效**的 alias
                     * （state.desktopIconStage），不能读用户当前选择的 state.stage：
                     * 图标要到 onStop 才落地，「简洁图标 + 四级 → 回首页切六级 →
                     * 立刻进设置页」这个序列里，读 state.stage 会写「六」，
                     * 而桌面此刻仍是「四」—— 又是「设置页显示旧值」那一类。
                     * 读不到实际状态（null）时退回按选择显示，不猜。
                     */
                    val desktopStage = state.desktopIconStage ?: state.stage
                    val simpleChar = if (desktopStage == WordDb.STAGE_CET6) "六" else "四"
                    Text(
                        text = when {
                            pending && state.iconStyle == LauncherIcons.STYLE_SIMPLE ->
                                "桌面图标待切换：现在是书本图标，回到桌面后变成「$simpleChar」"
                            pending ->
                                "桌面图标待切换：现在是简洁图标，回到桌面后变回书本图标"
                            state.iconStyle == LauncherIcons.STYLE_SIMPLE ->
                                "当前桌面显示：简洁图标「$simpleChar」（${
                                    WordDb.stageLabel(desktopStage)
                                }）"
                            else -> "当前桌面显示：默认书本图标"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pending) LocalAppColors.current.warning
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "选择后回到桌面即已生效，部分手机需要几秒钟刷新图标缓存。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- 离线说明 ----------
            item {
                AppCard(color = LocalAppColors.current.cardMint) {
                    SectionTitle(text = "离线运行说明", accent = LocalAppColors.current.success)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "词库、例句、词根与联想记忆内容全部内置于安装包，打开即用",
                        "安装包未申请INTERNET权限，系统层面无法联网",
                        "发音调用系统TTS引擎，同样无需联网",
                        "学习进度、成绩与成就仅保存在本机数据库，不上传任何服务器"
                    ).forEach {
                        BulletRow(
                            text = it,
                            modifier = Modifier.padding(vertical = 3.dp),
                            dotColor = LocalAppColors.current.success
                        )
                    }
                }
            }

            // ---------- 数据来源 ----------
            item {
                AppCard(color = LocalAppColors.current.cardSky) {
                    SectionTitle(text = "数据来源与授权")
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        // 逐条对应构建脚本 tools/build_words_db.py 的真实取材方式，
                        // 不写「官方词表」这类无法核对的说法：
                        // ECDICT 的 cet4/cet6 是社区维护字段，四/六级词表还并了教材词表，
                        // 六级表包含全部四级词。联想记忆含大模型生成内容，必须主动披露。
                        "四/六级词表：开源词典ECDICT（MIT许可）的cet4/cet6标签，" +
                            "并上开源词库english-vocabulary的教材词表后去重",
                        "六级词表包含全部四级词，两级的单词卡片各自独立成行",
                        "中文释义、英美音标、例句：开源词库english-vocabulary与ECDICT",
                        "词根词缀与形近词：由上述开源数据按词缀表自动构建",
                        "联想记忆：精选助记由大模型生成、人工校对后内置，" +
                            "其余由词根词缀与形近数据拼接，仅供记忆启发，不作为权威释义",
                        "图标与界面元素：应用内自行绘制，不含第三方图片素材"
                    ).forEach {
                        BulletRow(
                            text = it,
                            modifier = Modifier.padding(vertical = 3.dp),
                            dotColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ---------- 数据管理 ----------
            item {
                AppCard(color = LocalAppColors.current.cardRose) {
                    SectionTitle(
                        text = "数据管理",
                        accent = LocalAppColors.current.danger,
                        trailing = {
                            /*
                             * 进行中必须有明确反馈。此前唯一的表现是「四行变半透明、点不动」，
                             * 而备份 1 万条 word_state + study_log 要跑几秒 ——
                             * 用户看到按钮变灰会以为卡死或操作被吃掉，于是重复点击（无效），
                             * 或者去点同一张卡里那张红色的「清空全部学习数据」。
                             * 这里给一个转圈 + 一行状态文案，配合下面各行的禁用，
                             * 让「正在忙」这件事说不出口也能看懂。
                             */
                            if (fileBusy) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalAppColors.current.danger
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "处理中…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    ActionRow(
                        title = "导出学习记录（CSV）",
                        desc = "含熟练度等级、错误次数、上次学习与下次复习时间",
                        iconKind = 1,
                        enabled = !fileBusy,
                        onClick = { vm.exportStudyCsv() }
                    )
                    ActionRow(
                        title = "导出单词本（CSV）",
                        desc = "仅导出收藏的生词，按字母序排列",
                        iconKind = 2,
                        enabled = !fileBusy,
                        onClick = { vm.exportFavCsv() }
                    )
                    ActionRow(
                        title = "备份数据（JSON）",
                        desc = "完整备份学习进度、成绩、成就与设置",
                        iconKind = 3,
                        enabled = !fileBusy,
                        onClick = { vm.backupJson() }
                    )
                    ActionRow(
                        title = "恢复数据",
                        desc = "从备份文件恢复，将覆盖当前全部学习数据",
                        iconKind = 4,
                        enabled = !fileBusy,
                        onClick = { pickRestore() }
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalAppColors.current.danger.copy(alpha = 0.10f))
                            /*
                             * 这一行必须和上面四行一样受 fileBusy 约束。
                             * 它是设置页里最危险的操作，却偏偏是唯一没有 enabled 的：
                             * 备份/恢复跑到一半（界面只有变灰、没有任何进度）时，
                             * 用户很容易以为卡死、转而来点这张红色的清空卡。
                             * 两个写操作并发没有跨存储事务，会留下「数据是备份的、
                             * 设置被清成默认」这类半成品状态。
                             * 变灰用 alpha 与 ActionRow 保持同一套观感。
                             */
                            .alpha(if (fileBusy) 0.42f else 1f)
                            .clickable(enabled = !fileBusy, role = Role.Button) { showReset = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalAppColors.current.danger),
                            contentAlignment = Alignment.Center
                        ) {
                            ActionIcon(kind = 0)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "清空全部学习数据",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAppColors.current.danger
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                // 与弹窗里的 reset_confirm_message 保持一致：
                                // resetAll() 会 prefs.clear()，应用设置（级别、目标、语速、
                                // 图标、明暗模式）也会被一并清掉，这里不能只说学习数据。
                                text = "将删除学习进度、闯关成绩、成就记录与应用设置，不可恢复",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconChevronRight(
                            modifier = Modifier.size(16.dp),
                            color = LocalAppColors.current.danger
                        )
                    }
                }
            }

            // ---------- 关于 ----------
            item {
                AppCard(color = LocalAppColors.current.cardAmber) {
                    SectionTitle(text = "关于")
                    Spacer(Modifier.height(10.dp))
                    InfoRow("应用名称", "过四斩六")
                    InfoRow("版本", appVersionName() + "（离线版）")
                    InfoRow("网络权限", "未申请")
                    // 六级表是「cet6 标签 ∪ 四级表」，所以 6248 里含全部 4750 个四级词；
                    // 不写清楚容易被读成「两本共一万一千多个不同单词」。
                    InfoRow("词库规模", "四级4750词/六级6248词（六级含全部四级词）")
                    InfoRow("开发环境", "Kotlin + Jetpack Compose")
                }
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll()
                    showReset = false
                }) { Text(stringResource(R.string.action_reset_confirm), color = LocalAppColors.current.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // 导出/备份成功：展示绝对路径 + 复制路径 + 分享出去
    fileDone?.let { done ->
        val (title, path, file) = done
        AlertDialog(
            onDismissRequest = { vm.consumeFileDone() },
            title = { Text(title) },
            text = {
                Column {
                    Text(
                        text = "文件已保存到：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    /*
                     * 把「为什么还要一个分享按钮」写在界面上：
                     * Android 11+ 起 Android/data 在系统文件 App 与 USB MTP 里都不可见，
                     * 用户照着路径去文件管理器里找是找不到的。
                     */
                    Text(
                        text = "Android 11 起系统文件管理器看不到这个目录，想拿到电脑上，" +
                            "请用下面的「分享到…」发送出去。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.shareFile(file, title) }) {
                    Text("分享到…")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        // 复制成功要给一句反馈：剪贴板服务取不到时 copyPath 是静默返回的，
                        // 而此前点完按钮界面毫无变化，用户无法判断是否复制成功、通常连点几次。
                        val ok = copyPath(path)
                        vm.toast(if (ok) "路径已复制" else "这个设备取不到剪贴板服务")
                        vm.consumeFileDone()
                    }) { Text(stringResource(R.string.action_copy_path)) }
                    TextButton(onClick = { vm.consumeFileDone() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        )
    }

    // 文件任务失败
    fileError?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.consumeFileError() },
            title = { Text(stringResource(R.string.title_operation_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { vm.consumeFileError() }) { Text(stringResource(R.string.action_ok)) }
            }
        )
    }

    // 多个备份时让用户选择
    if (restoreChoices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { restoreChoices = emptyList() },
            title = { Text(stringResource(R.string.restore_pick_title)) },
            // 备份列表必须可滚：目录里攒十几个备份时，普通 Column 会把
            // 超出弹窗高度的条目直接裁在屏幕外，用户既看不到也点不到。
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    restoreChoices.forEach { f ->
                        Text(
                            text = f.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) {
                                    restoreTarget = f
                                    restoreChoices = emptyList()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { restoreChoices = emptyList() }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // 恢复前二次确认
    restoreTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Text(
                    "将用备份「${target.name}」覆盖当前全部学习数据" +
                        "（学习进度、答题记录、闯关成绩、成就与设置），此操作不可撤销。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreTarget = null
                    vm.restoreBackup(target)
                }) { Text(stringResource(R.string.action_restore_confirm), color = LocalAppColors.current.danger) }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // 备份来自另一版词库：数据层一行数据都还没动就抛回来，这里把差异说清楚再让用户决定。
    // 「仍要恢复」必须是明确的一次点击，不能默认放行 —— 旧备份往往只有这一份，
    // 直接拒掉等于把用户的进度锁在门外，但放行确实会有进度缺口，两害要让用户自己称。
    wordbookMismatch?.let { m ->
        AlertDialog(
            onDismissRequest = { vm.dismissWordbookMismatch() },
            title = { Text(stringResource(R.string.restore_wordbook_title)) },
            text = {
                Text(
                    m.detail + "\n\n" +
                        "仍要恢复的话，对不上当前词库的记录会被跳过，" +
                        "恢复结果可能出现进度缺口。建议先在电脑上打开这份备份确认词库版本。"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.restoreIgnoringWordbookMismatch() }) {
                    Text(stringResource(R.string.restore_wordbook_keep), color = LocalAppColors.current.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissWordbookMismatch() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionIcon(kind: Int) {
    // 描边必须跟随主题取反色。
    // 此前写死 Color.White，而底色用的是主题主色：深色主题下主色是较亮的橙
    // （DarkPrimary #FFF0855A），白描边压亮橙对比度极低，四个图标几乎看不清。
    val stroke = MaterialTheme.colorScheme.onPrimary
    val sw = 2.2.dp
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = size
        val strokeW = sw.toPx()
        when (kind) {
            1 -> { // 下载：向下箭头落到底线
                val ax = s.width / 2f
                drawLine(stroke, Offset(ax, s.height * 0.18f), Offset(ax, s.height * 0.70f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(s.width * 0.26f, s.height * 0.50f)
                    lineTo(ax, s.height * 0.74f)
                    lineTo(s.width * 0.74f, s.height * 0.50f)
                }
                drawPath(head, stroke,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(stroke, Offset(s.width * 0.22f, s.height * 0.90f), Offset(s.width * 0.78f, s.height * 0.90f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            2 -> { // 心形（收藏）
                val cx = s.width / 2f
                val cy = s.height * 0.55f
                val r = s.width * 0.18f
                val left = Path().apply {
                    moveTo(cx, s.height * 0.86f)
                    cubicTo(s.width * 0.05f, s.height * 0.62f,
                            s.width * 0.05f, s.height * 0.30f,
                            cx - r, cy - r * 0.2f)
                    cubicTo(s.width * 0.30f, s.height * 0.10f,
                            cx, cy - r * 0.5f,
                            cx, cy + r * 0.3f)
                    cubicTo(cx, cy - r * 0.5f,
                            s.width * 0.70f, s.height * 0.10f,
                            cx + r, cy - r * 0.2f)
                    cubicTo(s.width * 0.95f, s.height * 0.30f,
                            s.width * 0.95f, s.height * 0.62f,
                            cx, s.height * 0.86f)
                    close()
                }
                drawPath(left, stroke,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            3 -> { // 备份：磁盘（圆角矩形 + 上方缺口）
                val p = Path().apply {
                    moveTo(s.width * 0.18f, s.height * 0.32f)
                    lineTo(s.width * 0.36f, s.height * 0.18f)
                    lineTo(s.width * 0.82f, s.height * 0.18f)
                    lineTo(s.width * 0.82f, s.height * 0.82f)
                    lineTo(s.width * 0.18f, s.height * 0.82f)
                    close()
                }
                drawPath(p, stroke,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(stroke, Offset(s.width * 0.32f, s.height * 0.50f), Offset(s.width * 0.68f, s.height * 0.50f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            4 -> { // 恢复：向上箭头（从底线升起）
                val ax = s.width / 2f
                drawLine(stroke, Offset(ax, s.height * 0.30f), Offset(ax, s.height * 0.82f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(s.width * 0.26f, s.height * 0.50f)
                    lineTo(ax, s.height * 0.26f)
                    lineTo(s.width * 0.74f, s.height * 0.50f)
                }
                drawPath(head, stroke,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(stroke, Offset(s.width * 0.22f, s.height * 0.12f), Offset(s.width * 0.78f, s.height * 0.12f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            else -> { // 清空：垃圾桶
                val lx = s.width * 0.22f
                val rx = s.width * 0.78f
                drawLine(stroke, Offset(lx - s.width * 0.06f, s.height * 0.26f),
                    Offset(rx + s.width * 0.06f, s.height * 0.26f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(s.width * 0.40f, s.height * 0.12f),
                    Offset(s.width * 0.60f, s.height * 0.12f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(lx, s.height * 0.26f),
                    Offset(lx + s.width * 0.06f, s.height * 0.90f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(lx + s.width * 0.06f, s.height * 0.90f),
                    Offset(rx - s.width * 0.06f, s.height * 0.90f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(rx - s.width * 0.06f, s.height * 0.26f),
                    Offset(rx, s.height * 0.90f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(s.width * 0.40f, s.height * 0.40f),
                    Offset(s.width * 0.42f, s.height * 0.78f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(s.width * 0.50f, s.height * 0.40f),
                    Offset(s.width * 0.50f, s.height * 0.78f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(stroke, Offset(s.width * 0.58f, s.height * 0.40f),
                    Offset(s.width * 0.60f, s.height * 0.78f),
                    strokeWidth = strokeW, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    desc: String,
    iconKind: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.42f)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            ActionIcon(kind = iconKind)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconChevronRight(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 从包信息读取版本号，避免「关于」页写死的版本号与 build.gradle.kts 脱节
 * （曾因两版都显示 1.0.0，导致无法分辨装的是哪一版）。
 */
@Composable
private fun appVersionName(): String {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "未知"
    }
}

/**
 * 一个图标选项：上方是 1:1 的图标预览，下方是名称与说明，选中时描边高亮。
 */
@Composable
private fun IconOption(
    modifier: Modifier = Modifier,
    selected: Boolean,
    title: String,
    caption: String,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) accent.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                shape = RoundedCornerShape(16.dp)
            )
            // 二选一，用单选语义：TalkBack 会朗读「已选中 / 未选中」
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        preview()
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 预览框：56dp 圆角橙底，尺寸与桌面图标对齐。
 *
 * ⚠️ 前景必须按【启动器的裁法】画，不能整张 Fit：
 * 自适应图标的 108dp 画布只有中央 72dp 会露出来，启动器把那 72dp 放大铺满图标。
 * 若像 v1.4.2 之前那样把整张画布 Fit 进 48dp，预览里的字形只有桌面的 0.57 倍，
 * 用户看到的就是「桌面和设置里不一样」（2026-10-15 反馈）。
 * 做法：把前景按 56 * 108/72 = 84dp 居中铺出去，靠外层 clip 裁掉溢出，
 * 露出来的正好是画布中央 72/108，与桌面逐像素同比例。
 * ⚠️ 溢出必须用 requiredSize：Modifier.size(84.dp) 会被父框的 56dp 上限夹回去，
 * 于是又变回整张画布 Fit，实测字形只有桌面的 0.65 倍（第一版就踩了这个坑）。
 */
@Composable
private fun IconPreviewFrame(foreground: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2703A)),
        contentAlignment = Alignment.Center
    ) {
        foreground()
    }
}

/** 默认图标预览：品牌橙底 + 应用内绘制的书本前景（按启动器裁法放大到 84dp）。 */
@Composable
private fun DefaultIconPreview() {
    IconPreviewFrame {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(84.dp)
        )
    }
}

/**
 * 简洁图标预览：橙底白字，字形直接铺桌面图标同一个前景 PNG
 * （mipmap/ic_fg_si 或 ic_fg_liu），并按 IconPreviewFrame 的裁法还原启动器遮罩，
 * 保证预览与桌面所见完全一致。
 * ⚠️ 别再退回 Text 画字：系统默认字体与桌面图标生成用的黑体不是一回事，
 * 预览会和桌面「两张脸」（v1.3.0 的教训）。
 * ⚠️ 也别改回 Modifier.size(48.dp)：那是整张 108 画布，等于偷偷缩小 1.76 倍。
 */
@Composable
private fun SimpleIconPreview(foregroundRes: Int) {
    IconPreviewFrame {
        Image(
            painter = painterResource(foregroundRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(84.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
