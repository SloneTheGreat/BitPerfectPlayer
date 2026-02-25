package com.example.bitperfectplayer.audio

import android.content.Context
import android.media.*
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UsbDacManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class DacInfo(
        val device: AudioDeviceInfo,
        val supportedMixerAttributes: List<AudioMixerAttributes>,
        val bitPerfectAttribute: AudioMixerAttributes?,
        val formatInfo: String
    )

    fun getConnectedUsbDacs(): List<DacInfo> {
        val mediaAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        return audioManager.getAudioDevicesForAttributes(mediaAttr)
            .filter { it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                      it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                      it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY }
            .map { device ->
                val supported = audioManager.getSupportedMixerAttributes(device)
                val bitPerfect = supported.firstOrNull {
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }
                val rates = device.sampleRates.joinToString(" / ") { "${it/1000}kHz" }
                val encodings = device.encodings.joinToString(" / ") { enc ->
                    when(enc) {
                        AudioFormat.ENCODING_PCM_16BIT -> "16bit"
                        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24bit"
                        AudioFormat.ENCODING_PCM_32BIT -> "32bit"
                        AudioFormat.ENCODING_PCM_FLOAT -> "Float"
                        else -> "Enc($enc)"
                    }
                }
                DacInfo(device, supported, bitPerfect, "${device.productName} | $rates | $encodings")
            }
    }

    fun enableBitPerfect(dacInfo: DacInfo): Boolean {
        val bitPerfectAttr = dacInfo.bitPerfectAttribute ?: return false
        val mediaAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        return audioManager.setPreferredMixerAttributes(mediaAttr, dacInfo.device, bitPerfectAttr)
    }

    fun disableBitPerfect(device: AudioDeviceInfo) {
        val mediaAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        audioManager.clearPreferredMixerAttributes(mediaAttr, device)
    }
}
