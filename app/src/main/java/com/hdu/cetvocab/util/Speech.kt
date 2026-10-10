package com.hdu.cetvocab.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统 TTS 封装。使用手机自带的离线语音引擎，不产生任何网络请求，
 * 也不增加安装包体积。
 */
class Speech(context: Context, private val rateProvider: () -> Float) {

    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                runCatching {
                    engine?.language = Locale.US
                    engine?.setSpeechRate(rateProvider())
                    engine?.setPitch(1.0f)
                }
            }
        }
    }

    val isReady: Boolean get() = ready

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        runCatching {
            engine?.setSpeechRate(rateProvider())
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cet_" + text.hashCode())
        }
    }

    fun shutdown() {
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
        ready = false
    }
}
