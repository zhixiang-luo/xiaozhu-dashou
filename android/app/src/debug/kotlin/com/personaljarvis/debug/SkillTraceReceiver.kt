package com.personaljarvis.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.skills.SkillTraceSerializer
import com.personaljarvis.ui.FloatingBubbleService
import java.io.File

class SkillTraceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_RUN_TASK) {
            val instruction = intent
                .getStringExtra(FloatingBubbleService.EXTRA_INSTRUCTION)
                ?.trim()
                .orEmpty()
            if (instruction.isNotEmpty()) {
                runCatching {
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
                }
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
        const val EXTRA_RESET = "reset"
    }
}
