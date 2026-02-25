package com.example.bitperfectplayer.ui

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bitperfectplayer.MainActivity
import com.example.bitperfectplayer.databinding.FragmentLibraryBinding
import com.example.bitperfectplayer.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class LibraryFragment : Fragment() {
    private lateinit var binding: FragmentLibraryBinding
    private lateinit var viewModel: PlayerViewModel
    private lateinit var adapter: AudioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        viewModel = (activity as MainActivity).viewModel
        adapter = AudioAdapter { file -> viewModel.play(file); (activity as MainActivity).selectTab(1) }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.scanButton.setOnClickListener { scanMediaLibrary() }
        return binding.root
    }

    private fun scanMediaLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                val result = mutableListOf<AudioFile>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.DATA
                )
                val cursor = context?.contentResolver?.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                    MediaStore.Audio.Media.DISPLAY_NAME
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        result.add(AudioFile(
                            id = it.getLong(0), uri = MediaStore.Audio.Media.getContentUri("external", it.getLong(0)),
                            name = it.getString(1) ?: "", artist = it.getString(2) ?: "Unknown",
                            album = it.getString(3) ?: "Unknown", durationMs = it.getLong(4),
                            mimeType = it.getString(5) ?: "audio/mpeg", filePath = it.getString(6) ?: ""
                        ))
                    }
                }
                result
            }
            adapter.submitList(files)
            viewModel.setAudioFiles(files)
        }
    }
}

class AudioAdapter(private val onItemClick: (AudioFile) -> Unit) :
    ListAdapter<AudioFile, AudioAdapter.ViewHolder>(object : DiffUtil.ItemCallback<AudioFile>() {
        override fun areItemsTheSame(o: AudioFile, n: AudioFile) = o.id == n.id
        override fun areContentsTheSame(o: AudioFile, n: AudioFile) = o == n
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(file: AudioFile) {
            itemView.findViewById<TextView>(android.R.id.text1).text = file.name
            itemView.findViewById<TextView>(android.R.id.text2).text = "${file.artist} • ${file.formatBadge} • ${file.formattedDuration}"
            itemView.setOnClickListener { onItemClick(file) }
        }
    }
}
