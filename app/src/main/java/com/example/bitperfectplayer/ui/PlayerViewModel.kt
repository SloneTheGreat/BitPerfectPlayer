package com.example.bitperfectplayer.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bitperfectplayer.audio.BitPerfectAudioEngine
import com.example.bitperfectplayer.model.AudioFile

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PlayerViewModel(private val context: Context) : ViewModel() {
    private val audioEngine = BitPerfectAudioEngine(context)
    private val _audioFiles = MutableLiveData<List<AudioFile>>(emptyList())
    val audioFiles: LiveData<List<AudioFile>> = _audioFiles
    private val _selectedFile = MutableLiveData<AudioFile?>(null)
    val selectedFile: LiveData<AudioFile?> = _selectedFile
    private val _playbackState = MutableLiveData(BitPerfectAudioEngine.State.IDLE)
    val playbackState: LiveData<BitPerfectAudioEngine.State> = _playbackState
    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress
    private val _sampleRate = MutableLiveData("")
    val sampleRate: LiveData<String> = _sampleRate
    private val _bitDepth = MutableLiveData("")
    val bitDepth: LiveData<String> = _bitDepth
    private val _channels = MutableLiveData("")
    val channels: LiveData<String> = _channels
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    private val _dacInfo = MutableLiveData("")
    val dacInfo: LiveData<String> = _dacInfo
    init {
        audioEngine.onStateChanged = { _playbackState.postValue(it) }
        audioEngine.onError = { _errorMessage.postValue(it) }
        audioEngine.onProgress = { _progress.postValue(it) }
        audioEngine.onFormatResolved = { sr, bd, ch ->
            _sampleRate.postValue("$sr Hz"); _bitDepth.postValue("$bd-bit")
            _channels.postValue(if (ch == 1) "Mono" else "$ch-Ch")
        }
        audioEngine.onDacConnected = { _dacInfo.postValue("${it.device.productName} (Bit-Perfect Ready)") }
    }
    fun setAudioFiles(files: List<AudioFile>) { _audioFiles.value = files }
    fun play(file: AudioFile) { _selectedFile.value = file; audioEngine.play(file.uri) }
    fun pause() { audioEngine.pause() }
    fun resume() { audioEngine.resume() }
    fun stop() { audioEngine.stop() }
    override fun onCleared() { audioEngine.release(); super.onCleared() }
}
