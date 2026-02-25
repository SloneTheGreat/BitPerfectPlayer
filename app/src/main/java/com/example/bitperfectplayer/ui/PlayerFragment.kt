package com.example.bitperfectplayer.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.bitperfectplayer.MainActivity
import com.example.bitperfectplayer.audio.BitPerfectAudioEngine
import com.example.bitperfectplayer.databinding.FragmentPlayerBinding

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PlayerFragment : Fragment() {
    private lateinit var binding: FragmentPlayerBinding
    private lateinit var viewModel: PlayerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        viewModel = (activity as MainActivity).viewModel
        binding.playButton.setOnClickListener {
            if (viewModel.playbackState.value == BitPerfectAudioEngine.State.PLAYING) viewModel.pause()
            else viewModel.resume()
        }
        viewModel.selectedFile.observe(viewLifecycleOwner) {
            binding.titleText.text = it?.name ?: "No track selected"
            binding.artistText.text = it?.artist ?: ""
        }
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            binding.statusBadge.text = when (state) {
                BitPerfectAudioEngine.State.PLAYING -> "🔴 BIT-PERFECT PLAYING"
                BitPerfectAudioEngine.State.PAUSED -> "⏸ PAUSED"
                BitPerfectAudioEngine.State.NO_DAC -> "⚠ NO DAC CONNECTED"
                BitPerfectAudioEngine.State.PREPARING -> "⏳ PREPARING..."
                else -> "⏹ STOPPED"
            }
            binding.playButton.text = if (state == BitPerfectAudioEngine.State.PLAYING) "PAUSE" else "PLAY"
        }
        viewModel.sampleRate.observe(viewLifecycleOwner) { binding.sampleRateText.text = "SR: $it" }
        viewModel.bitDepth.observe(viewLifecycleOwner) { binding.bitDepthText.text = "BD: $it" }
        viewModel.channels.observe(viewLifecycleOwner) { binding.channelsText.text = "CH: $it" }
        viewModel.dacInfo.observe(viewLifecycleOwner) { binding.dacInfoText.text = it }
        return binding.root
    }
}
