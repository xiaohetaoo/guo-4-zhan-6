package com.hdu.cetvocab.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统 TTS 封装。使用手机自带的离线语音引擎，不产生任何网络请求，
 * 也不增加安装包体积。
 *
 * 对外提供两个入口，把「自动朗读」开关的判断收敛到这里：
 * - [speak]      手动发音（用户点喇叭），始终朗读；
 * - [speakAuto]  自动发音（翻卡、出题时触发），遵守设置里的开关。
 *
 * 历史问题：判断散落在各个页面里，`QuizScreen` 漏判了 `autoSpeak`，
 * 导致用户关掉「自动朗读」后闯关拼写题仍然出声。收敛到本类后不会再漏。
 */
class Speech(
    context: Context,
    private val rateProvider: () -> Float,
    private val autoSpeakProvider: () -> Boolean
) {

    /**
     * TTS 引擎。初始化回调运行在 TTS 自己的线程上，而读写发生在主线程，
     * 因此两个字段都标注 @Volatile —— 否则主线程可能长期读不到 `ready = true`
     * （可见性问题），表现为「喇叭点了没反应」。
     */
    @Volatile
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready = false

    /** 是否已经设置过语言 / 语速 / 音调 */
    @Volatile
    private var configured = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) ensureConfigured()
        }
    }

    val isReady: Boolean get() = ready

    /**
     * 补齐语言 / 语速 / 音调设置。
     *
     * 放在这里而不是只在初始化回调里做，是因为回调有可能早于 `engine` 赋值触发
     * （构造函数的返回值还没赋给字段）。此时 `engine` 为 null，配置会被跳过；
     * 之后每次发音再补一次，保证最终一定配置成功。
     */
    private fun ensureConfigured() {
        if (configured) return
        val tts = engine ?: return
        runCatching {
            /*
             * setLanguage 的返回值不能丢。旧代码 `tts.language = Locale.US` 直接把
             * 返回值扔掉并无条件 configured = true，于是引擎里没有英文语音数据时
             * （LANG_MISSING_DATA / LANG_NOT_SUPPORTED，国产 ROM 上并不罕见）
             * 表现是：用中文音色硬读英文单词、且因为 configured 已置位而永不重试，
             * 用户只会觉得「读得不准」，不知道要去装语音包。
             * 这里记下缺失状态，交给界面提示一次；语速仍照常设置，
             * 因为语速与语言是否可用无关。
             */
            val r = tts.setLanguage(Locale.US)
            englishVoiceMissing = r == TextToSpeech.LANG_MISSING_DATA ||
                r == TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(rateProvider())
            tts.setPitch(1.0f)
            configured = true
        }
    }

    /** 英文语音数据是否缺失（未初始化前为 false） */
    @Volatile
    var englishVoiceMissing: Boolean = false
        private set

    /**
     * 发音不可用时的回调（引擎没装好、或英文语音数据缺失）。
     * 由 AppViewModel 挂上，用来提示一次；不在此类里直接弹 Toast，
     * 保持 Speech 不依赖界面层。
     */
    var onUnavailable: ((String) -> Unit)? = null

    /** 手动发音：用户主动点击喇叭，不受「自动朗读」开关影响 */
    fun speak(text: String) {
        if (text.isBlank()) return
        ensureConfigured()
        val tts = engine
        if (!ready || tts == null) {
            // 点了没反应比「读得难听」更让人以为 App 坏了，必须给一句反馈
            onUnavailable?.invoke("系统语音引擎未就绪，稍后再试一次")
            return
        }
        runCatching {
            // 语速可能在设置里被改过，每次发音前重新同步
            tts.setSpeechRate(rateProvider())
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cet_" + text.hashCode())
        }
        if (englishVoiceMissing) {
            onUnavailable?.invoke("系统未安装英文语音数据，发音可能不准；可在系统设置的语音输入里安装")
        }
    }

    /** 自动发音：翻卡 / 出题时触发，遵守「自动朗读」开关 */
    fun speakAuto(text: String) {
        if (!autoSpeakProvider()) return
        // 自动发音不打扰：引擎缺失时静默跳过，避免每翻一张卡弹一次提示
        if (!ready || englishVoiceMissing) return
        speak(text)
    }

    fun shutdown() {
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
        ready = false
        configured = false
        englishVoiceMissing = false
    }
}
