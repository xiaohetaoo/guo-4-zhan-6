package com.hdu.cetvocab.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 闯关答题提示音。
 *
 * 为什么现场合成而不是放两段 wav：
 * 本项目对素材口径的要求是「界面与图标全部应用内自绘，不含第三方素材」，
 * 音效同理。合成两段各不到 0.2 秒的 PCM 只要几十行代码，安装包一个字节都不增，
 * 也不用为「音效素材来自哪里、授权如何」再写一段声明。
 * 另外把波形收在这里，音量与音色是可控的：答对明亮上行、答错低沉短促，
 * 响度对齐到不会在公共场合吓人的程度（峰值约 -12 dBFS）。
 *
 * 播放走 USAGE_MEDIA，因此跟随媒体音量：媒体音量静音时自然无声，
 * 不需要额外做一个音量滑块。通话中（mode != NORMAL）直接不播，避免抢音。
 */
class Sfx(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "cetvocab-sfx").apply { isDaemon = true }
    }

    /** 设置页的「答题音效」开关。volatile：主线程写，播放线程读。 */
    @Volatile
    var enabled: Boolean = true

    private val correctPcm: ShortArray by lazy { tone(listOf(880.0 to 0.07, 1318.0 to 0.12), harmonics = 1.0) }
    private val wrongPcm: ShortArray by lazy { tone(listOf(233.0 to 0.09, 175.0 to 0.13), harmonics = 0.42) }

    /** 正在播的那条音轨。只在播放线程上读写，不需要锁。 */
    private var pending: AudioTrack? = null

    /**
     * 生成一段 PCM。
     *
     * [segments] 是「频率 + 时长」的串联，两个音之间不留间隙 —— 听起来才像一声，
     * 而不是两声。包络用「3 ms 淡入 + 指数衰减」，直接按方波幅度硬切会在起止点
     * 产生可闻的咔哒声（小音量下尤其明显，用户会以为是 bug 而不是音效）。
     * [harmonics] 给二次谐波的权重：1.0 接近纯音、听感干净（答对）；
     * 0.42 会让音色发闷、略带沙感，用来表达「错了」比纯音更合适。
     */
    private fun tone(segments: List<Pair<Double, Double>>, harmonics: Double): ShortArray {
        val total = (SAMPLE_RATE * segments.sumOf { it.second }).toInt()
        val buf = ShortArray(total)
        val attack = (SAMPLE_RATE * 0.003).toInt().coerceAtLeast(1)
        var pos = 0
        for ((freq, seconds) in segments) {
            val len = (SAMPLE_RATE * seconds).toInt()
            for (i in 0 until len) {
                val t = (pos + i) / SAMPLE_RATE.toDouble()
                val global = (pos + i)
                var sample = sin(2.0 * PI * freq * t)
                if (harmonics < 1.0) sample += harmonics * sin(2.0 * PI * freq * 2.0 * t)
                // 段内衰减 + 起止淡变：淡入看全局位置，淡出留给下一段衔接，
                // 只有最后一段做完整淡出，否则两段之间会被切出爆音。
                var env = exp(-3.0 * (i.toDouble() / len))
                if (global < attack) env *= global.toDouble() / attack
                val fadeOutFrom = total - attack
                if (global > fadeOutFrom) env *= ((total - global).toDouble() / attack).coerceIn(0.0, 1.0)
                val v = (sample * env * PEAK * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767)
                buf[(pos + i).coerceAtMost(total - 1)] = v.toShort()
            }
            pos += len
        }
        return buf
    }

    /** 答对 / 答错各播一声；[correct] 决定音色。 */
    fun play(correct: Boolean) {
        if (!enabled) return
        val pcm = if (correct) correctPcm else wrongPcm
        executor.execute { runCatching { write(pcm) } }
    }

    private fun write(pcm: ShortArray) {
        if (audioManager?.mode != null && audioManager.mode != AudioManager.MODE_NORMAL) return
        // 上一声的释放回调还没来就又响一声（连点选项时会同帧触发）：
        // 先把上一条音轨收掉。音轨数量有系统上限，漏十几条之后音效会静默失效，
        // 那种「用着用着没声了」的 bug 最难查，宁可丢掉半秒尾音。
        pending?.let { runCatching { it.release() } }
        pending = null
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (e: Exception) {
            // 音频轨耗尽、被系统策略拒绝等：静默跳过，答题本身绝不能被音效拖住
            return
        }
        try {
            pending = track
            track.write(pcm, 0, pcm.size)
            // 播完即释放。用「播放位置标记 + 位置回调」而不是 setPlaybackCompletionListener：
            // 后者在本工程 compileSdk 下不可见，而每答一题建一条音轨、不释放的话
            // 十几题就会撞上系统 AudioTrack 数量上限，之后音效静默失效。
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack) {
                        if (pending === t) pending = null
                        runCatching { t.release() }
                    }

                    override fun onPeriodicNotification(t: AudioTrack) = Unit
                }
            )
            track.play()
        } catch (e: Exception) {
            if (pending === track) pending = null
            runCatching { track.release() }
        }
    }

    fun shutdown() {
        // 用 shutdown() 而不是 shutdownNow()：后者会中断队列里还没跑的释放任务，
        // 音轨就漏在系统里了。这里排的就是释放任务，必须让它执行完。
        executor.execute {
            pending?.let { runCatching { it.release() } }
            pending = null
        }
        executor.shutdown()
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        /** 峰值幅度：约 -12 dBFS，够听见又不至于突然炸一下 */
        private const val PEAK = 0.25
    }
}
