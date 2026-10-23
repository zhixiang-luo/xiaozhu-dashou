package com.personaljarvis.observe

data class UiBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class RawUiNode(
    val role: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: UiBounds,
    val visible: Boolean,
    val clickable: Boolean,
    val editable: Boolean,
    val actions: Set<String> = emptySet(),
    val sensitive: Boolean = false
)

data class UiNodeSnapshot(
    val nodeId: String,
    val role: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: UiBounds,
    val clickable: Boolean,
    val editable: Boolean,
    val actions: Set<String>
)
