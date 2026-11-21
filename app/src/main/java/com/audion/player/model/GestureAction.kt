package com.audion.player.model

enum class GestureAction(val label: String) {
    NONE("Tidak ada aksi"),
    PLAY_PAUSE("Putar / Jeda"),
    NEXT_TRACK("Lagu berikutnya"),
    PREVIOUS_TRACK("Lagu sebelumnya"),
    FORWARD("Maju 10 detik"),
    REWIND("Mundur 10 detik"),
    READ_NOW_PLAYING("Bacakan info lagu saat ini");

    companion object {
        fun fromName(name: String?, default: GestureAction): GestureAction {
            if (name.isNullOrBlank()) return default
            return entries.find { it.name == name } ?: default
        }
    }
}