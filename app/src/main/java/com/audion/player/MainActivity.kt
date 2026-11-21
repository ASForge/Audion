package com.audion.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.audion.player.adapter.FolderAdapter
import com.audion.player.databinding.ActivityMainBinding
import com.audion.player.engine.AudioPlayerEngine
import com.audion.player.gesture.GesturePadView
import com.audion.player.gesture.GesturePreferences
import com.audion.player.model.AudioTrack
import com.audion.player.model.GestureAction
import com.audion.player.model.MusicFolder
import com.audion.player.scanner.MusicFolderScanner
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: MusicFolderScanner
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var playerEngine: AudioPlayerEngine
    private lateinit var gesturePrefs: GesturePreferences

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
        gesturePrefs = GesturePreferences(this)

        playerEngine = AudioPlayerEngine(this) { track, isPlaying, folderName ->
            updatePlayerUi(track, isPlaying, folderName)
        }

        folderAdapter = FolderAdapter { selectedFolder ->
            playerEngine.playFolder(selectedFolder, startIndex = 0)
        }

        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = folderAdapter

        setupControls()
        setupGesturePad()

        binding.btnScanFolders.setOnClickListener {
            checkPermissionAndScan()
        }

        binding.btnConfigureGestures.setOnClickListener {
            showGestureMenuDialog()
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

    private fun setupGesturePad() {
        binding.gesturePad.setGestureListener(object : GesturePadView.Listener {
            override fun onTwoFingerTap() {
                executeGestureAction(gesturePrefs.getTwoFingerTapAction())
            }

            override fun onTwoFingerSwipeLeft() {
                executeGestureAction(gesturePrefs.getTwoFingerSwipeLeftAction())
            }

            override fun onTwoFingerSwipeRight() {
                executeGestureAction(gesturePrefs.getTwoFingerSwipeRightAction())
            }

            override fun onPadDoubleTap() {
                executeGestureAction(gesturePrefs.getDoubleTapPadAction())
            }
        })
    }

    private fun executeGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> {
                binding.tvStatus.text = "Gestur ini belum diatur aksinya."
            }
            GestureAction.PLAY_PAUSE -> {
                playerEngine.togglePlayPause()
            }
            GestureAction.NEXT_TRACK -> {
                playerEngine.nextTrack()
            }
            GestureAction.PREVIOUS_TRACK -> {
                playerEngine.previousTrack()
            }
            GestureAction.FORWARD -> {
                playerEngine.forward()
                binding.tvStatus.text = "Maju 10 detik"
            }
            GestureAction.REWIND -> {
                playerEngine.rewind()
                binding.tvStatus.text = "Mundur 10 detik"
            }
            GestureAction.READ_NOW_PLAYING -> {
                val current = playerEngine.getCurrentTrack()
                binding.tvStatus.text = if (current != null) {
                    "Sedang diputar: ${current.title}, oleh ${current.artist}, album ${current.album}."
                } else {
                    "Belum ada lagu yang sedang diputar."
                }
            }
        }
    }

    private fun showGestureMenuDialog() {
        val gestureSlots = arrayOf(
            "Ketuk 2 jari (saat ini: ${gesturePrefs.getTwoFingerTapAction().label})",
            "Geser 2 jari ke kiri (saat ini: ${gesturePrefs.getTwoFingerSwipeLeftAction().label})",
            "Geser 2 jari ke kanan (saat ini: ${gesturePrefs.getTwoFingerSwipeRightAction().label})",
            "Ketuk 2 kali di panel gestur (saat ini: ${gesturePrefs.getDoubleTapPadAction().label})"
        )

        AlertDialog.Builder(this)
            .setTitle("Pengaturan Gestur Audion")
            .setItems(gestureSlots) { _, which ->
                when (which) {
                    0 -> showActionPicker("Ketuk 2 jari", gesturePrefs.getTwoFingerTapAction()) {
                        gesturePrefs.setTwoFingerTapAction(it)
                    }
                    1 -> showActionPicker("Geser 2 jari ke kiri", gesturePrefs.getTwoFingerSwipeLeftAction()) {
                        gesturePrefs.setTwoFingerSwipeLeftAction(it)
                    }
                    2 -> showActionPicker("Geser 2 jari ke kanan", gesturePrefs.getTwoFingerSwipeRightAction()) {
                        gesturePrefs.setTwoFingerSwipeRightAction(it)
                    }
                    3 -> showActionPicker("Ketuk 2 kali di panel gestur", gesturePrefs.getDoubleTapPadAction()) {
                        gesturePrefs.setDoubleTapPadAction(it)
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showActionPicker(
        gestureTitle: String,
        currentAction: GestureAction,
        onSelected: (GestureAction) -> Unit
    ) {
        val actions = GestureAction.entries
        val labels = actions.map { it.label }.toTypedArray()
        val checkedIndex = actions.indexOf(currentAction).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Pilih aksi untuk: $gestureTitle")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, idx ->
                val chosen = actions[idx]
                onSelected(chosen)
                binding.tvStatus.text = "$gestureTitle diatur ke: ${chosen.label}"
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
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