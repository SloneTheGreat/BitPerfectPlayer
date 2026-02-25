package com.example.bitperfectplayer.audio

import android.content.Context
import android.media.*
import android.net.Uri
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class AudioFileDecoder(private val context: Context) {

    data class AudioMetadata(
        val sampleRate: Int,
        val channelCount: Int,
        val encoding: Int,
        val durationMs: Long,
        val mimeType: String,
        val bitRate: Int
    )

    suspend fun decode(uri: Uri, onProgress: ((Int) -> Unit)? = null): Pair<ByteArray, AudioMetadata>? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var audioTrackIdx = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIdx = i; audioFormat = fmt; break
            }
        }
        if (audioTrackIdx < 0 || audioFormat == null) { extractor.release(); return@withContext null }
        extractor.selectTrack(audioTrackIdx)
        val mime       = audioFormat.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCnt = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val encoding   = if (audioFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) audioFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
        val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) audioFormat.getLong(MediaFormat.KEY_DURATION) else 0L
        val bitRate    = if (audioFormat.containsKey(MediaFormat.KEY_BIT_RATE)) audioFormat.getInteger(MediaFormat.KEY_BIT_RATE) else 0
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(audioFormat, null, null, 0)
        codec.start()
        val output = mutableListOf<Byte>()
        val info = MediaCodec.BufferInfo()
        var eos = false
        while (!eos && isActive) {
            val inIdx = codec.dequeueInputBuffer(10_000)
            if (inIdx >= 0) {
                val buf: ByteBuffer = codec.getInputBuffer(inIdx)!!
                val sz = extractor.readSampleData(buf, 0)
                if (sz < 0) { codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos = true }
                else { codec.queueInputBuffer(inIdx, 0, sz, extractor.sampleTime, 0); extractor.advance()
                    if (durationUs > 0) onProgress?.invoke((extractor.sampleTime * 100 / durationUs).toInt().coerceIn(0, 99)) }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            if (outIdx >= 0) {
                val buf: ByteBuffer = codec.getOutputBuffer(outIdx)!!
                val chunk = ByteArray(info.size); buf.get(chunk); output.addAll(chunk.toList())
                codec.releaseOutputBuffer(outIdx, false)
            }
        }
        codec.stop(); codec.release(); extractor.release(); onProgress?.invoke(100)
        Pair(output.toByteArray(), AudioMetadata(sampleRate, channelCnt, encoding, durationUs/1000, mime, bitRate))
    }
}
