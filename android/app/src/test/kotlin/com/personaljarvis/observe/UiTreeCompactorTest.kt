package com.personaljarvis.observe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTreeCompactorTest {
    private val compactor = UiTreeCompactor()

    @Test
    fun removesInvisibleAndNonInteractiveNodes() {
        val output = compactor.compact(
            listOf(
                rawNode(role = "button", clickable = true),
                rawNode(role = "text", visible = false),
                rawNode(role = "label")
            )
        )

        assertEquals(listOf("button-1"), output.map { it.nodeId })
    }

    @Test
    fun assignsStableIdsForTheSameScreenOrder() {
        val input = listOf(
            rawNode(role = "button", clickable = true),
            rawNode(role = "edit", editable = true)
        )

        val first = compactor.compact(input)
        val second = compactor.compact(input)

        assertEquals(first.map { it.nodeId }, second.map { it.nodeId })
        assertEquals(listOf("button-1", "edit-2"), first.map { it.nodeId })
    }

    @Test
    fun capsLargeTreesToVisibleInteractiveNodes() {
        val input = List(150) { rawNode(role = "button", clickable = true) }

        val output = compactor.compact(input)

        assertEquals(UiTreeCompactor.MAX_NODES, output.size)
        assertTrue(output.all { it.clickable })
    }

    private fun rawNode(
        role: String,
        visible: Boolean = true,
        clickable: Boolean = false,
        editable: Boolean = false
    ) = RawUiNode(
        role = role,
        text = role,
        contentDescription = null,
        bounds = UiBounds(0, 0, 100, 100),
        visible = visible,
        clickable = clickable,
        editable = editable
    )
}
