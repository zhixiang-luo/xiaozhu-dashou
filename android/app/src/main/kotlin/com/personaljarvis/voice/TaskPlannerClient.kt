package com.personaljarvis.voice

import com.personaljarvis.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PlannedAction(
    val actionId: String,
    val taskId: String,
    val taskGeneration: Int,
    val type: String,
    val expectedState: String,
    val expectedPackage: String? = null,
    val targetPackage: String? = null,
    val targetX: Double? = null,
    val targetY: Double? = null,
    val text: String? = null,
    val question: String? = null
)

sealed interface PlannerResult {
    data class Plan(val actions: List<PlannedAction>) : PlannerResult
    data class Message(val text: String) : PlannerResult
    data class Failure(val reason: String) : PlannerResult
}

fun interface TaskTransport {
    fun createTask(instruction: String): String
}

class TaskPlannerClient(
    private val transport: TaskTransport = HttpTaskTransport(
        baseUrl = BuildConfig.ORCHESTRATOR_URL,
        deviceToken = BuildConfig.DEVICE_TOKEN
    )
) {
    fun submit(instruction: String): PlannerResult {
        return runCatching {
            parseResponse(transport.createTask(instruction.trim()))
        }.getOrElse {
            PlannerResult.Failure(it.message ?: "planner_request_failed")
        }
    }

    private fun parseResponse(raw: String): PlannerResult {
        val root = JSONObject(raw)
        val actions = root.getJSONArray("actions")
        if (actions.length() == 0) {
            return PlannerResult.Failure("planner_returned_no_actions")
        }

        val plan = actions.length().let { length ->
            buildList {
                for (index in 0 until length) {
                    add(parseAction(actions.getJSONObject(index)))
                }
            }
        }

        val message = plan.firstOrNull { it.type == "ask_user" }
            ?.question
            ?.let(PlannerResult::Message)
        return message ?: PlannerResult.Plan(plan)
    }

    private fun parseAction(action: JSONObject): PlannedAction {
        val type = action.getString("type")
        val target = action.optJSONObject("target")
        return PlannedAction(
            actionId = action.getString("actionId"),
            taskId = action.getString("taskId"),
            taskGeneration = action.getInt("taskGeneration"),
            type = type,
            expectedState = action.getString("expectedState"),
            expectedPackage = action.optString("expectedPackage").ifBlank { null },
            targetPackage = action.optString("packageName").ifBlank { null },
            targetX = target?.optDouble("x")?.takeUnless { it.isNaN() },
            targetY = target?.optDouble("y")?.takeUnless { it.isNaN() },
            text = action.optString("text").ifBlank { null },
            question = action.optString("question").ifBlank { null }
        )
    }
}

class HttpTaskTransport(
    private val baseUrl: String,
    private val deviceToken: String
) : TaskTransport {
    override fun createTask(instruction: String): String {
        val url = URL("${baseUrl.trimEnd('/')}/v1/tasks")
        require(
            url.protocol == "https" ||
                url.host == "127.0.0.1" ||
                url.host == "localhost"
        ) {
            "Only HTTPS or loopback development endpoints are allowed"
        }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("authorization", "Bearer $deviceToken")
            setRequestProperty("content-type", "application/json")
        }

        try {
            connection.outputStream.use { stream ->
                stream.write(
                    JSONObject()
                        .put("instruction", instruction)
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(status in 200..299) {
                "planner_http_$status:$body"
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
