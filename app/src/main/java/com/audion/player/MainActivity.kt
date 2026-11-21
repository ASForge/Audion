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
import com.audion.player.model.MusicFolder
import com.audion.player.scanner.MusicFolderScanner
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: MusicFolderScanner
    private lateinit var folderAdapter: FolderAdapter

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
        folderAdapter = FolderAdapter { selectedFolder ->
            onFolderSelected(selectedFolder)
        }

        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = folderAdapter

        binding.btnScanFolders.setOnClickListener {
            checkPermissionAndScan()
        }

        checkPermissionAndScan()
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

    private fun onFolderSelected(folder: MusicFolder) {
        val firstTrack = folder.tracks.firstOrNull() ?: return
        binding.tvNowPlaying.text =
            "Folder: ${folder.folderName}\nJudul: ${firstTrack.title}\nPenyanyi: ${firstTrack.artist}\nAlbum: ${firstTrack.album}"
    }
}