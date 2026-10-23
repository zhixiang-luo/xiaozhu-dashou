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
    val targetPackage: String? = null,
    val question: String? = null
)

sealed interface PlannerResult {
    data class Action(val action: PlannedAction) : PlannerResult
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

        val action = actions.getJSONObject(0)
        return when (action.getString("type")) {
            "open_app" -> PlannerResult.Action(
                PlannedAction(
                    actionId = action.getString("actionId"),
                    taskId = action.getString("taskId"),
                    taskGeneration = action.getInt("taskGeneration"),
                    type = "open_app",
                    expectedState = action.getString("expectedState"),
                    targetPackage = action.getString("packageName")
                )
            )
            "ask_user" -> PlannerResult.Message(action.getString("question"))
            else -> PlannerResult.Failure(
                "unsupported_action:${action.getString("type")}"
            )
        }
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
