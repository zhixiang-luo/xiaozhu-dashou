package com.personaljarvis.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.skills.SkillTraceSerializer
import com.personaljarvis.ui.FloatingBubbleService
import com.personaljarvis.wechat.WechatConversationReader
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class SkillTraceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_CAPTURE_WECHAT_CONTEXT) {
            val pending = goAsync()
            Thread {
                val reader = WechatConversationReader()
                try {
                    val state = reader.read()
                    val directory = File(context.filesDir, "debug")
                        .apply { mkdirs() }
                    File(directory, "wechat-context.json").writeText(
                        if (state == null) {
                            "null"
                        } else {
                            JSONObject()
                                .put("title", state.title)
                                .put(
                                    "lines",
                                    JSONArray().apply {
                                        state.lines.forEach { line ->
                                            put(line.text)
                                        }
                                    }
                                )
                                .toString()
                        }
                    )
                } finally {
                    reader.close()
                    pending.finish()
                }
            }.start()
            return
        }
        if (intent?.action == ACTION_RUN_TASK) {
            val instruction = intent
                .getStringExtra(FloatingBubbleService.EXTRA_INSTRUCTION)
                ?.trim()
                .orEmpty()
            if (instruction.isNotEmpty()) {
                val started = runCatching {
                    context.startForegroundService(
                        Intent(context, FloatingBubbleService::class.java)
                            .setAction(
                                FloatingBubbleService.ACTION_RUN_INSTRUCTION
                            )
                            .putExtra(
                                FloatingBubbleService.EXTRA_INSTRUCTION,
                                instruction
                            )
                    )
                }.isSuccess
                Log.d(DEBUG_LOG_TAG, "debug_task_started=$started")
            }
            return
        }
        if (intent?.action != ACTION_TRACE) {
            return
        }

        val traceDirectory = File(context.filesDir, "skill-trace").apply { mkdirs() }
        val output = File(traceDirectory, "latest.jsonl")
        if (intent.getBooleanExtra(EXTRA_RESET, false)) {
            output.writeText("")
            return
        }

        val snapshot = AccessibilityObserver.instance?.snapshot() ?: return
        output.appendText(
            SkillTraceSerializer.toJson(snapshot, System.currentTimeMillis()) + "\n"
        )
    }

    companion object {
        const val ACTION_TRACE = "com.personaljarvis.DEBUG_SKILL_TRACE"
        const val ACTION_RUN_TASK = "com.personaljarvis.DEBUG_RUN_TASK"
        const val ACTION_CAPTURE_WECHAT_CONTEXT =
            "com.personaljarvis.DEBUG_CAPTURE_WECHAT_CONTEXT"
        const val EXTRA_RESET = "reset"
        private const val DEBUG_LOG_TAG = "XiaozhuDashou"
    }
}
