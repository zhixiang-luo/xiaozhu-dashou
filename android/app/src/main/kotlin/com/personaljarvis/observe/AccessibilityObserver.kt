package com.personaljarvis.observe

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.ObservationSnapshot
import com.personaljarvis.policy.PageClassifier
import com.personaljarvis.policy.RecipientResolution
import java.security.MessageDigest

class AccessibilityObserver : AccessibilityService() {
    private val compactor = UiTreeCompactor()
    private val nodesById = mutableMapOf<String, AccessibilityNodeInfo>()

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        nodesById.clear()
        super.onDestroy()
    }

    fun snapshot(): ObservationSnapshot? {
        val root = rootInActiveWindow ?: return null
        val packageName = root.packageName?.toString() ?: return null
        val collected = mutableListOf<Pair<RawUiNode, AccessibilityNodeInfo>>()
        collect(root, collected)

        val eligible = collected.filter { (raw, _) ->
            raw.visible &&
                (raw.clickable || raw.editable || raw.actions.isNotEmpty())
        }
        val snapshots = compactor.compact(eligible.map { it.first })

        nodesById.clear()
        snapshots.forEachIndexed { index, snapshot ->
            nodesById[snapshot.nodeId] = eligible[index].second
        }

        val texts = snapshots.mapNotNull { it.text ?: it.contentDescription }
        val hasSensitiveInput = eligible.any { it.first.sensitive }
        val pageKind = PageClassifier.classify(
            packageName = packageName,
            texts = texts,
            hasSensitiveInput = hasSensitiveInput
        )

        return ObservationSnapshot(
            foregroundPackage = packageName,
            pageTitle = texts.firstOrNull(),
            pageKind = pageKind,
            recipientResolution = RecipientResolution.NONE,
            nodes = snapshots,
            screenHash = hashSnapshot(packageName, snapshots)
        )
    }

    fun performTap(nodeId: String): DriverResult {
        val node = nodesById[nodeId]
            ?: return DriverResult.Failure("node_not_found")
        return if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            DriverResult.Success
        } else {
            DriverResult.Failure("click_action_failed")
        }
    }

    fun performText(nodeId: String, text: String): DriverResult {
        val node = nodesById[nodeId]
            ?: return DriverResult.Failure("node_not_found")
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return if (
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        ) {
            DriverResult.Success
        } else {
            DriverResult.Failure("set_text_action_failed")
        }
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        output: MutableList<Pair<RawUiNode, AccessibilityNodeInfo>>
    ) {
        if (output.size >= UiTreeCompactor.MAX_NODES) {
            return
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val actions = buildSet {
            if (node.isClickable) add("click")
            if (node.isEditable) add("set_text")
        }
        val sensitive = node.isPassword
        output.add(
            RawUiNode(
                role = node.className?.toString()?.substringAfterLast('.') ?: "node",
                text = if (sensitive) null else node.text?.toString(),
                contentDescription = if (sensitive) {
                    null
                } else {
                    node.contentDescription?.toString()
                },
                bounds = UiBounds(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom
                ),
                visible = node.isVisibleToUser,
                clickable = node.isClickable,
                editable = node.isEditable,
                actions = actions,
                sensitive = sensitive
            ) to node
        )

        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collect(child, output)
            }
        }
    }

    private fun hashSnapshot(
        packageName: String,
        nodes: List<UiNodeSnapshot>
    ): String {
        val source = buildString {
            append(packageName)
            nodes.forEach { node ->
                append(node.role)
                append(node.text)
                append(node.contentDescription)
                append(node.bounds)
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    companion object {
        @Volatile
        var instance: AccessibilityObserver? = null
            private set
    }
}
