package com.example.bitperfectplayer.model

import android.net.Uri

data class AudioFile(
    val id: Long,
    val uri: Uri,
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val filePath: String = ""
) {
    val formattedDuration: String get() {
        val minutes = durationMs / 60000
        val seconds = (durationMs % 60000) / 1000
        return "%d:%02d".format(minutes, seconds)
    }
    val formatBadge: String get() = when {
        mimeType.contains("flac") -> "FLAC"
        mimeType.contains("wav")  -> "WAV"
        mimeType.contains("aiff") -> "AIFF"
        mimeType.contains("mpeg") -> "MP3"
        mimeType.contains("aac") || mimeType.contains("mp4") -> "AAC"
        else -> "PCM"
    }
}
