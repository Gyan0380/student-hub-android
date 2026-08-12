package com.studenthub.app.util

object Slug {
    fun slugify(input: String): String {
        return input.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    fun classRoomId(classLevel: String): String {
        return "class-" + slugify(classLevel)
    }
}
