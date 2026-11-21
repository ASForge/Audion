package com.audion.player.model

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val folderPath: String,
    val folderName: String,
    val storageType: String
)