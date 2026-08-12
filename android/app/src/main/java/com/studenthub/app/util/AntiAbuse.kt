package com.studenthub.app.util

object AntiAbuse {
    /**
     * Client-side UX-only check. The real enforcement happens in Firestore security rules
     * and/or backend functions. This is only meant to warn the user before sending.
     */
    fun containsBannedWord(text: String, bannedWords: List<String>): Boolean {
        if (bannedWords.isEmpty()) return false
        val lower = text.lowercase()
        return bannedWords.any { word ->
            word.isNotBlank() && lower.contains(word.lowercase())
        }
    }
}
