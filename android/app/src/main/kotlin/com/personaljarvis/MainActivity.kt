package com.personaljarvis

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.personaljarvis.execute.ShizukuUserServiceBridge
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.policy.PageKind
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private lateinit var shizukuBridge: ShizukuUserServiceBridge
    private var shizukuStatus by mutableStateOf("")

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shizukuBridge.connect()
                shizukuStatus = getString(R.string.shizuku_authorized)
            } else {
                shizukuStatus = getString(R.string.shizuku_permission_denied)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shizukuStatus = getString(R.string.shizuku_not_connected)
        shizukuBridge = ShizukuUserServiceBridge(this) { available ->
            shizukuStatus = if (available) {
                getString(R.string.shizuku_authorized)
            } else {
                getString(R.string.shizuku_disconnected)
            }
        }
        Shizuku.addRequestPermissionResultListener(
            permissionListener,
            Handler(Looper.getMainLooper())
        )
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PersonalJarvisScreen(
                        shizukuStatus = shizukuStatus,
                        requestShizukuAccess = ::requestShizukuAccess
                    )
                }
            }
        }
        if (Shizuku.pingBinder() && hasShizukuPermission()) {
            shizukuBridge.connect()
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    private fun requestShizukuAccess() {
        shizukuStatus = when {
            !Shizuku.pingBinder() -> getString(R.string.shizuku_not_running)
            hasShizukuPermission() -> {
                shizukuBridge.connect()
                if (shizukuBridge.isAvailable) {
                    getString(R.string.shizuku_authorized)
                } else {
                    getString(R.string.shizuku_connecting)
                }
            }
            else -> {
                Shizuku.requestPermission(1001)
                getString(R.string.shizuku_waiting_permission)
            }
        }
    }

    private fun hasShizukuPermission(): Boolean {
        return runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }
}

@Composable
fun PersonalJarvisScreen(
    shizukuStatus: String,
    requestShizukuAccess: () -> Unit
) {
    val context = LocalContext.current
    var taskActive by remember { mutableStateOf(false) }
    var screenStatus by remember {
        mutableStateOf(context.getString(R.string.screen_not_observed))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = androidx.compose.ui.Alignment.CenterVertically
        )
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = if (taskActive) {
                stringResource(R.string.status_listening)
            } else {
                stringResource(R.string.status_ready)
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(onClick = { taskActive = !taskActive }) {
            Text(
                if (taskActive) {
                    stringResource(R.string.action_stop_task)
                } else {
                    stringResource(R.string.action_start_task)
                }
            )
        }
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        ) {
            Text(stringResource(R.string.action_enable_accessibility))
        }
        Button(
            onClick = {
                val observation = AccessibilityObserver.instance?.snapshot()
                screenStatus = if (observation == null) {
                    context.getString(R.string.accessibility_inactive)
                } else {
                    "当前应用：${observation.foregroundPackage}，" +
                        "页面：${pageKindLabel(observation.pageKind)}，" +
                        "控件：${observation.nodes.size} 个"
                }
            }
        ) {
            Text(stringResource(R.string.action_read_screen))
        }
        Text(text = screenStatus)
        Button(onClick = requestShizukuAccess) {
            Text(stringResource(R.string.action_authorize_shizuku))
        }
        Text(text = shizukuStatus)
    }
}

private fun pageKindLabel(pageKind: PageKind): String {
    return when (pageKind) {
        PageKind.NORMAL -> "普通页面"
        PageKind.LOGIN -> "登录页面"
        PageKind.PAYMENT -> "支付页面"
        PageKind.OTP -> "验证码页面"
        PageKind.SECURE -> "安全页面"
        PageKind.PERMISSION -> "权限页面"
    }
}
