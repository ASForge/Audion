package com.audion.player.scanner

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.audion.player.model.AudioTrack
import com.audion.player.model.MusicFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicFolderScanner(private val context: Context) {

    suspend fun scanAllFolders(): List<MusicFolder> = withContext(Dispatchers.IO) {
        val folderMap = LinkedHashMap<String, MutableList<AudioTrack>>()
        val storageMap = HashMap<String, String>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
                add(MediaStore.Audio.Media.VOLUME_NAME)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1

            val volumeCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)

                val rawTitle = cursor.getString(titleCol)?.trim()
                val displayName = cursor.getString(displayNameCol)?.substringBeforeLast('.') ?: "Tanpa Judul"
                val title = if (!rawTitle.isNullOrEmpty()) rawTitle else displayName

                val rawArtist = cursor.getString(artistCol)?.trim()
                val artist = if (!rawArtist.isNullOrEmpty() && rawArtist != "<unknown>") {
                    rawArtist
                } else {
                    "Artis tidak diketahui"
                }

                val rawAlbum = cursor.getString(albumCol)?.trim()
                val album = if (!rawAlbum.isNullOrEmpty() && rawAlbum != "<unknown>") {
                    rawAlbum
                } else {
                    "Album tidak diketahui"
                }

                val durationMs = cursor.getLong(durationCol)

                val volumeName = if (volumeCol >= 0) {
                    cursor.getString(volumeCol) ?: "external_primary"
                } else {
                    "external_primary"
                }

                val storageType = if (volumeName.equals("external_primary", ignoreCase = true)) {
                    "Penyimpanan Internal"
                } else {
                    "Kartu SD / Eksternal ($volumeName)"
                }

                val relPath = if (relPathCol >= 0) {
                    cursor.getString(relPathCol)?.trim('/') ?: ""
                } else ""

                val bucketName = cursor.getString(bucketCol)?.trim()
                val folderName = when {
                    !bucketName.isNullOrEmpty() -> bucketName
                    relPath.isNotEmpty() -> relPath.substringAfterLast('/')
                    else -> "Root ($storageType)"
                }

                val folderKey = "$volumeName::$relPath"

                val track = AudioTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    uri = contentUri,
                    folderPath = if (relPath.isNotEmpty()) "/$relPath" else "/",
                    folderName = folderName,
                    storageType = storageType
                )

                folderMap.getOrPut(folderKey) { mutableListOf() }.add(track)
                storageMap[folderKey] = storageType
            }
        }

        folderMap.map { (folderKey, tracks) ->
            val sample = tracks.first()
            MusicFolder(
                folderPath = sample.folderPath,
                folderName = sample.folderName,
                storageType = storageMap[folderKey] ?: sample.storageType,
                tracks = tracks
            )
        }.sortedBy { it.folderName.lowercase() }
    }
}