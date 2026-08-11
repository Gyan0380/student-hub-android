package com.studenthub.app.data.model

/** Mirrors Tags/{id}: { id, label, color, type }. Used by the admin panel to assign
 *  badges (e.g. "Verified", "Helper") to users. */
data class AppTag(
    val id: String = "",
    val label: String = "",
    val color: String = "#2563EB",
    val type: String = "general"
)
