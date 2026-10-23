package com.personaljarvis.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.skills.SkillTraceSerializer
import java.io.File

class SkillTraceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
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
        const val EXTRA_RESET = "reset"
    }
}
