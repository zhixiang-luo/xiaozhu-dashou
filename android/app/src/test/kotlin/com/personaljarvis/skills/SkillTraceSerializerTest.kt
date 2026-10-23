package com.personaljarvis.skills

import com.personaljarvis.observe.UiBounds
import com.personaljarvis.observe.UiNodeSnapshot
import com.personaljarvis.policy.ObservationSnapshot
import com.personaljarvis.policy.PageKind
import com.personaljarvis.policy.RecipientResolution
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SkillTraceSerializerTest {
    @Test
    fun serializesObservationWithNodeSelectors() {
        val snapshot = ObservationSnapshot(
            foregroundPackage = "com.tencent.mm",
            pageTitle = "文件传输助手",
            pageKind = PageKind.NORMAL,
            recipientResolution = RecipientResolution.NONE,
            nodes = listOf(
                UiNodeSnapshot(
                    nodeId = "edit-1",
                    role = "EditText",
                    text = "测试",
                    contentDescription = "输入",
                    bounds = UiBounds(10, 20, 300, 100),
                    clickable = true,
                    editable = true,
                    actions = setOf("click", "set_text")
                )
            ),
            screenHash = "abc123"
        )

        val json = JSONObject(SkillTraceSerializer.toJson(snapshot, 1234L))

        assertEquals("com.tencent.mm", json.getString("package"))
        assertEquals(1234L, json.getLong("timestamp"))
        assertEquals(
            "测试",
            json.getJSONArray("nodes").getJSONObject(0).getString("text")
        )
    }
}
