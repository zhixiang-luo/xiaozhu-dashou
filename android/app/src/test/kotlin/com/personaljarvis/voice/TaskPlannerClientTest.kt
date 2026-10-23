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

        assertTrue(result is PlannerResult.Action)
        assertEquals(
            "com.android.settings",
            (result as PlannerResult.Action).action.targetPackage
        )
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
