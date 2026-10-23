package com.personaljarvis.memory

import org.json.JSONArray
import org.json.JSONObject

data class CalendarContextEvent(
    val title: String,
    val startTime: String,
    val endTime: String,
    val timeZone: String
)

data class RecentTaskContext(
    val summary: String,
    val occurredAt: String
)

data class RelevantMemoryContext(
    val kind: String,
    val summary: String,
    val occurredAt: String
)

data class ContactContext(
    val alias: String,
    val canonicalName: String
)

data class PreferenceContext(
    val key: String,
    val value: String
)

data class WechatMessagePayload(
    val direction: String,
    val text: String
)

data class CurrentConversationPayload(
    val title: String,
    val appPackage: String,
    val recentMessages: List<WechatMessagePayload>,
    val capturedAt: String
)

data class TaskContextPayload(
    val currentTime: String,
    val timeZone: String,
    val currentPackage: String? = null,
    val screenSummary: String? = null,
    val currentConversation: CurrentConversationPayload? = null,
    val upcomingEvents: List<CalendarContextEvent>,
    val recentTasks: List<RecentTaskContext>,
    val relevantMemory: List<RelevantMemoryContext>,
    val contacts: List<ContactContext>,
    val preferences: List<PreferenceContext>
) {
    fun toJson(): String {
        return JSONObject()
            .put("currentTime", currentTime)
            .put("timeZone", timeZone)
            .apply {
                currentPackage?.let { put("currentPackage", it) }
                screenSummary?.let { put("screenSummary", it) }
                currentConversation?.let { conversation ->
                    put(
                        "currentConversation",
                        JSONObject()
                            .put("title", conversation.title)
                            .put("appPackage", conversation.appPackage)
                            .put("capturedAt", conversation.capturedAt)
                            .put(
                                "recentMessages",
                                JSONArray().apply {
                                    conversation.recentMessages.forEach { message ->
                                        put(
                                            JSONObject()
                                                .put("direction", message.direction)
                                                .put("text", message.text)
                                        )
                                    }
                                }
                            )
                    )
                }
            }
            .put(
                "upcomingEvents",
                JSONArray().apply {
                    upcomingEvents.forEach { event ->
                        put(
                            JSONObject()
                                .put("title", event.title)
                                .put("startTime", event.startTime)
                                .put("endTime", event.endTime)
                                .put("timeZone", event.timeZone)
                        )
                    }
                }
            )
            .put(
                "recentTasks",
                JSONArray().apply {
                    recentTasks.forEach { task ->
                        put(
                            JSONObject()
                                .put("summary", task.summary)
                                .put("occurredAt", task.occurredAt)
                        )
                    }
                }
            )
            .put(
                "relevantMemory",
                JSONArray().apply {
                    relevantMemory.forEach { memory ->
                        put(
                            JSONObject()
                                .put("kind", memory.kind)
                                .put("summary", memory.summary)
                                .put("occurredAt", memory.occurredAt)
                        )
                    }
                }
            )
            .put(
                "contacts",
                JSONArray().apply {
                    contacts.forEach { contact ->
                        put(
                            JSONObject()
                                .put("alias", contact.alias)
                                .put("canonicalName", contact.canonicalName)
                        )
                    }
                }
            )
            .put(
                "preferences",
                JSONArray().apply {
                    preferences.forEach { preference ->
                        put(
                            JSONObject()
                                .put("key", preference.key)
                                .put("value", preference.value)
                        )
                    }
                }
            )
            .toString()
    }
}
