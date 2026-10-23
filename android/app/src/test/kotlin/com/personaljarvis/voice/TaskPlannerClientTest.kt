package com.personaljarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerClientTest {
    @Test
    fun parsesOpenAppAction() {
        val client = TaskPlannerClient {
            """
            {
              "task": {
                "id": "task-1",
                "generation": 0
              },
              "actions": [
                {
                  "type": "open_app",
                  "actionId": "action-1",
                  "taskId": "task-1",
                  "taskGeneration": 0,
                  "expectedState": "app.opened",
                  "timeoutMs": 10000,
                  "packageName": "com.android.settings"
                }
              ]
            }
            """.trimIndent()
        }

        val result = client.submit("打开设置")

        assertTrue(result is PlannerResult.Plan)
        assertEquals(
            "com.android.settings",
            (result as PlannerResult.Plan).actions.single().targetPackage
        )
    }

    @Test
    fun parsesAnOrderedActionSequence() {
        val client = TaskPlannerClient {
            """
            {
              "task": {"id":"task-1","generation":0},
              "actions": [
                {"type":"open_app","actionId":"a1","taskId":"task-1","taskGeneration":0,"expectedState":"open","timeoutMs":10000,"packageName":"com.tencent.mm"},
                {"type":"tap","actionId":"a2","taskId":"task-1","taskGeneration":0,"expectedState":"chat","timeoutMs":5000,"expectedPackage":"com.tencent.mm","target":{"x":0.32,"y":0.26}},
                {"type":"input_text","actionId":"a3","taskId":"task-1","taskGeneration":0,"expectedState":"draft","timeoutMs":5000,"expectedPackage":"com.tencent.mm","text":"测试"}
              ]
            }
            """.trimIndent()
        }

        val result = client.submit("发送测试消息") as PlannerResult.Plan

        assertEquals(3, result.actions.size)
        assertEquals(0.32, result.actions[1].targetX)
        assertEquals("com.tencent.mm", result.actions[1].expectedPackage)
        assertEquals("测试", result.actions[2].text)
    }

    @Test
    fun parsesAskUserActionAsMessage() {
        val client = TaskPlannerClient {
            """
            {
              "task": {"id":"task-1","generation":0},
              "actions": [{
                "type":"ask_user",
                "actionId":"action-1",
                "taskId":"task-1",
                "taskGeneration":0,
                "expectedState":"user_input",
                "timeoutMs":30000,
                "question":"暂不支持这个任务"
              }]
            }
            """.trimIndent()
        }

        assertEquals(
            PlannerResult.Message("暂不支持这个任务"),
            client.submit("未知任务")
        )
    }

    @Test
    fun reportsMalformedResponses() {
        val client = TaskPlannerClient { "not-json" }

        assertTrue(client.submit("打开设置") is PlannerResult.Failure)
    }
}
