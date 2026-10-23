package com.personaljarvis.skills

import com.personaljarvis.policy.ObservationSnapshot
import org.json.JSONArray
import org.json.JSONObject

object SkillTraceSerializer {
    fun toJson(snapshot: ObservationSnapshot, timestamp: Long): String {
        return JSONObject()
            .put("timestamp", timestamp)
            .put("package", snapshot.foregroundPackage)
            .put("pageTitle", snapshot.pageTitle)
            .put("pageKind", snapshot.pageKind.name)
            .put("screenHash", snapshot.screenHash)
            .put("nodes", JSONArray().apply {
                snapshot.nodes.forEach { node ->
                    put(
                        JSONObject()
                            .put("nodeId", node.nodeId)
                            .put("role", node.role)
                            .put("text", node.text)
                            .put("contentDescription", node.contentDescription)
                            .put("left", node.bounds.left)
                            .put("top", node.bounds.top)
                            .put("right", node.bounds.right)
                            .put("bottom", node.bounds.bottom)
                            .put("clickable", node.clickable)
                            .put("editable", node.editable)
                            .put("actions", JSONArray(node.actions.toList()))
                    )
                }
            })
            .toString()
    }
}
