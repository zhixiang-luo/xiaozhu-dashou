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
import androidx.compose.ui.unit.dp
import com.personaljarvis.execute.ShizukuUserServiceBridge
import com.personaljarvis.observe.AccessibilityObserver
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private lateinit var shizukuBridge: ShizukuUserServiceBridge
    private var shizukuStatus by mutableStateOf("Shizuku not connected")

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shizukuBridge.connect()
                shizukuStatus = "Shizuku authorized"
            } else {
                shizukuStatus = "Shizuku permission denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shizukuBridge = ShizukuUserServiceBridge(this) { available ->
            shizukuStatus = if (available) {
                "Shizuku authorized"
            } else {
                "Shizuku disconnected"
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
            !Shizuku.pingBinder() -> "Shizuku is not running"
            hasShizukuPermission() -> {
                shizukuBridge.connect()
                if (shizukuBridge.isAvailable) {
                    "Shizuku authorized"
                } else {
                    "Shizuku connecting"
                }
            }
            else -> {
                Shizuku.requestPermission(1001)
                "Waiting for Shizuku permission"
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
    var taskActive by remember { mutableStateOf(false) }
    var screenStatus by remember { mutableStateOf("Screen not observed") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = androidx.compose.ui.Alignment.CenterVertically
        )
    ) {
        Text(
            text = AppIdentity.displayName,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = if (taskActive) "Listening for your next task" else "Ready",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(onClick = { taskActive = !taskActive }) {
            Text(if (taskActive) "Stop task" else "Start task")
        }
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        ) {
            Text("Enable accessibility")
        }
        Button(
            onClick = {
                val observation = AccessibilityObserver.instance?.snapshot()
                screenStatus = if (observation == null) {
                    "Accessibility service is not active"
                } else {
                    "${observation.foregroundPackage}: " +
                        "${observation.pageKind}, ${observation.nodes.size} nodes"
                }
            }
        ) {
            Text("Read current screen")
        }
        Text(text = screenStatus)
        Button(onClick = requestShizukuAccess) {
            Text("Authorize Shizuku")
        }
        Text(text = shizukuStatus)
    }
}
