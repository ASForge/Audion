package com.audion.player.model

data class MusicFolder(
    val folderPath: String,
    val folderName: String,
    val storageType: String,
    val tracks: List<AudioTrack>
) {
    val trackCount: Int get() = tracks.size
}