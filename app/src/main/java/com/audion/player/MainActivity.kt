package com.audion.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.audion.player.adapter.FolderAdapter
import com.audion.player.databinding.ActivityMainBinding
import com.audion.player.engine.AudioPlayerEngine
import com.audion.player.model.AudioTrack
import com.audion.player.model.MusicFolder
import com.audion.player.scanner.MusicFolderScanner
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: MusicFolderScanner
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var playerEngine: AudioPlayerEngine

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startFolderScan()
        } else {
            binding.tvStatus.text = "Izin akses audio ditolak. Aktifkan izin Audio di Pengaturan aplikasi untuk memindai folder."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanner = MusicFolderScanner(this)

        playerEngine = AudioPlayerEngine(this) { track, isPlaying, folderName ->
            updatePlayerUi(track, isPlaying, folderName)
        }

        folderAdapter = FolderAdapter { selectedFolder ->
            playerEngine.playFolder(selectedFolder, startIndex = 0)
        }

        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = folderAdapter

        setupControls()

        binding.btnScanFolders.setOnClickListener {
            checkPermissionAndScan()
        }

        checkPermissionAndScan()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            playerEngine.togglePlayPause()
        }

        binding.btnPrevious.setOnClickListener {
            playerEngine.previousTrack()
        }

        binding.btnNext.setOnClickListener {
            playerEngine.nextTrack()
        }

        binding.btnRewind.setOnClickListener {
            playerEngine.rewind()
            binding.tvStatus.text = "Mundur 10 detik"
        }

        binding.btnForward.setOnClickListener {
            playerEngine.forward()
            binding.tvStatus.text = "Maju 10 detik"
        }
    }

    private fun updatePlayerUi(track: AudioTrack?, isPlaying: Boolean, folderName: String?) {
        if (track == null) {
            binding.tvTrackTitle.text = "Judul Lagu: Belum ada lagu diputar"
            binding.tvTrackArtist.text = "Penyanyi: -"
            binding.tvTrackAlbum.text = "Album: -"
            binding.btnPlayPause.text = "Play"
            binding.btnPlayPause.contentDescription = "Putar lagu"
            return
        }

        binding.tvTrackTitle.text = "Judul Lagu: ${track.title}"
        binding.tvTrackArtist.text = "Penyanyi: ${track.artist}"
        binding.tvTrackAlbum.text = "Album: ${track.album}"

        if (isPlaying) {
            binding.btnPlayPause.text = "Pause"
            binding.btnPlayPause.contentDescription = "Jeda lagu ${track.title}"
            if (!folderName.isNullOrBlank()) {
                binding.tvStatus.text = "Memutar dari folder $folderName"
            }
        } else {
            binding.btnPlayPause.text = "Play"
            binding.btnPlayPause.contentDescription = "Putar lagu ${track.title}"
            if (!folderName.isNullOrBlank()) {
                binding.tvStatus.text = "Dijeda di folder $folderName"
            }
        }
    }

    private fun checkPermissionAndScan() {
        val permission = Manifest.permission.READ_MEDIA_AUDIO
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                startFolderScan()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                binding.tvStatus.text = "Aplikasi memerlukan izin membaca file audio untuk mengelompokkan lagu per folder."
                audioPermissionLauncher.launch(permission)
            }
            else -> {
                audioPermissionLauncher.launch(permission)
            }
        }
    }

    private fun startFolderScan() {
        binding.tvStatus.text = "Sedang memindai folder musik..."
        lifecycleScope.launch {
            val folders = scanner.scanAllFolders()
            folderAdapter.submitList(folders)

            val totalTracks = folders.sumOf { it.trackCount }
            binding.tvStatus.text = if (folders.isEmpty()) {
                "Tidak ditemukan folder musik."
            } else {
                "Ditemukan ${folders.size} folder dengan total $totalTracks lagu."
            }
        }
    }

    override fun onDestroy() {
        playerEngine.release()
        super.onDestroy()
    }
}