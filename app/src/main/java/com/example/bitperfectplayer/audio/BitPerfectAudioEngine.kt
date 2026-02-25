package com.example.bitperfectplayer.audio

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class BitPerfectAudioEngine(private val context: Context) {

    enum class State { IDLE, PREPARING, PLAYING, PAUSED, STOPPED, NO_DAC, ERROR }

    private val usbDacManager = UsbDacManager(context)
    private val decoder = AudioFileDecoder(context)
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var currentDac: UsbDacManager.DacInfo? = null

    var onStateChanged: ((State) -> Unit)? = null
    var onDacConnected: ((UsbDacManager.DacInfo) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onProgress: ((Int) -> Unit)? = null
    var onFormatResolved: ((Int, Int, Int) -> Unit)? = null

    private var state = State.IDLE
        set(v) { field = v; onStateChanged?.invoke(v) }

    fun initializeDac(): Boolean {
        val dacs = usbDacManager.getConnectedUsbDacs()
        val dac = dacs.firstOrNull { it.bitPerfectAttribute != null }
        return if (dac != null) {
            usbDacManager.enableBitPerfect(dac); currentDac = dac; onDacConnected?.invoke(dac); true
        } else {
            state = State.NO_DAC; onError?.invoke("비트퍼펙트를 지원하는 USB DAC가 연결되지 않았습니다."); false
        }
    }

    fun play(uri: Uri) {
        if (state == State.PAUSED) { resume(); return }
        stop()
        if (!initializeDac()) return
        val dac = currentDac ?: return
        val mixerAttr = dac.bitPerfectAttribute ?: return
        state = State.PREPARING
        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val (pcm, meta) = decoder.decode(uri) { onProgress?.invoke(it / 2) }
                    ?: throw Exception("파일 디코딩 실패")
                val audioFormat = mixerAttr.format
                val bufSize = AudioTrack.getMinBufferSize(audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding).coerceAtLeast(8192) * 4
                withContext(Dispatchers.Main) {
                    onFormatResolved?.invoke(audioFormat.sampleRate, encodingToBits(audioFormat.encoding), Integer.bitCount(audioFormat.channelMask))
                }
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                    .setAudioFormat(audioFormat).setBufferSizeInBytes(bufSize).setTransferMode(AudioTrack.MODE_STREAM).build()
                withContext(Dispatchers.Main) { state = State.PLAYING }
                audioTrack?.play()
                var offset = 0
                while (offset < pcm.size && isActive) {
                    val end = minOf(offset + bufSize, pcm.size)
                    audioTrack?.write(pcm, offset, end - offset); offset = end
                    withContext(Dispatchers.Main) { onProgress?.invoke(50 + (offset * 50 / pcm.size)) }
                }
                withContext(Dispatchers.Main) { onProgress?.invoke(100); state = State.STOPPED }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError?.invoke(e.message ?: "오류"); state = State.ERROR }
            }
        }
    }

    fun pause() { audioTrack?.pause(); state = State.PAUSED }
    fun resume() { audioTrack?.play(); state = State.PLAYING }
    fun stop() {
        playbackJob?.cancel(); playbackJob = null
        audioTrack?.stop(); audioTrack?.release(); audioTrack = null
        if (state != State.IDLE) state = State.STOPPED
    }
    fun release() { stop(); currentDac?.device?.let { usbDacManager.disableBitPerfect(it) }; currentDac = null }
    private fun encodingToBits(enc: Int) = when(enc) {
        AudioFormat.ENCODING_PCM_16BIT -> 16
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
        AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
        else -> 16
    }
}
