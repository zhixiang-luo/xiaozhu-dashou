package com.personaljarvis.observe

class UiTreeCompactor {
    fun compact(nodes: List<RawUiNode>): List<UiNodeSnapshot> {
        return nodes.asSequence()
            .filter { it.visible }
            .filter {
                it.clickable ||
                    it.editable ||
                    it.actions.isNotEmpty()
            }
            .take(MAX_NODES)
            .mapIndexed { index, node ->
                UiNodeSnapshot(
                    nodeId = "${idPrefix(node.role)}-${index + 1}",
                    role = node.role,
                    text = node.text,
                    contentDescription = node.contentDescription,
                    bounds = node.bounds,
                    clickable = node.clickable,
                    editable = node.editable,
                    actions = node.actions
                )
            }
            .toList()
    }

    private fun idPrefix(role: String): String {
        return role.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "node" }
    }

    companion object {
        const val MAX_NODES = 120
    }
}
