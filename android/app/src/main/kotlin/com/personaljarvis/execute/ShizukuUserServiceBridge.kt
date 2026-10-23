package com.personaljarvis.execute

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.personaljarvis.IShizukuActionService
import com.personaljarvis.ShizukuActionService
import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

class ShizukuUserServiceBridge(
    private val context: Context,
    private val onConnectionChanged: (Boolean) -> Unit = {}
) : ShizukuBridge {
    private var service: IShizukuActionService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.let(IShizukuActionService.Stub::asInterface)
            onConnectionChanged(service != null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            onConnectionChanged(false)
        }
    }

    private val userServiceArgs = UserServiceArgs(
        ComponentName(context, ShizukuActionService::class.java)
    )
        .daemon(false)
        .version(1)
        .tag("personal-jarvis-actions")
        .processNameSuffix("jarvis-actions")

    override val isAvailable: Boolean
        get() =
            Shizuku.pingBinder() &&
                hasPermission() &&
                service != null

    fun connect() {
        if (
            !Shizuku.pingBinder() ||
            !hasPermission() ||
            service != null
        ) {
            return
        }
        runCatching {
            Shizuku.bindUserService(userServiceArgs, connection)
        }
    }

    override fun execute(action: RuntimeAction): DriverResult {
        val activeService = service
            ?: return DriverResult.Unsupported("shizuku_user_service_unavailable")

        val success = when (action.type) {
            RuntimeActionType.OPEN_APP -> {
                val packageName = action.targetPackage
                    ?: return DriverResult.Failure("target_package_missing")
                activeService.openApp(packageName)
            }
            RuntimeActionType.TAP -> {
                val x = action.targetX
                    ?: return DriverResult.Failure("tap_x_missing")
                val y = action.targetY
                    ?: return DriverResult.Failure("tap_y_missing")
                val metrics = context.resources.displayMetrics
                activeService.tap(
                    (x.coerceIn(0f, 1f) * metrics.widthPixels).toInt(),
                    (y.coerceIn(0f, 1f) * metrics.heightPixels).toInt()
                )
            }
            RuntimeActionType.INPUT_TEXT -> {
                val text = action.text
                    ?: return DriverResult.Failure("input_text_missing")
                activeService.inputText(text)
            }
            RuntimeActionType.MEDIA_CONTROL,
            RuntimeActionType.SEND_MESSAGE ->
                return DriverResult.Unsupported("specialized_adapter_required")
        }

        return if (success) {
            DriverResult.Success
        } else {
            DriverResult.Failure("shizuku_action_failed")
        }
    }

    override fun cancel() = Unit

    private fun hasPermission(): Boolean {
        return runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }
}
