package com.personaljarvis.ui

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import com.personaljarvis.BuildConfig
import com.personaljarvis.MainActivity
import com.personaljarvis.R
import com.personaljarvis.execute.AccessibilityDriver
import com.personaljarvis.execute.FallbackExecutionDriver
import com.personaljarvis.execute.ShizukuDriver
import com.personaljarvis.execute.ShizukuUserServiceBridge
import com.personaljarvis.observe.AccessibilityObserver
import com.personaljarvis.policy.ActionResult
import com.personaljarvis.policy.ObservationSnapshot
import com.personaljarvis.policy.PageKind
import com.personaljarvis.policy.RecipientResolution
import com.personaljarvis.policy.RuntimeAction
import com.personaljarvis.policy.RuntimeActionType
import com.personaljarvis.policy.SafeExecutionRunner
import com.personaljarvis.voice.OkHttpVoiceStream
import com.personaljarvis.voice.PcmAudioCapture
import com.personaljarvis.voice.PlannerResult
import com.personaljarvis.voice.RealtimeVoiceSession
import com.personaljarvis.voice.TaskPlannerClient
import com.personaljarvis.voice.VoiceSessionScheduler
import com.personaljarvis.voice.VoiceSessionController
import kotlin.math.min
import kotlin.math.abs

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var shizukuBridge: ShizukuUserServiceBridge
    private val mainHandler = Handler(Looper.getMainLooper())
    private val taskPlannerClient = TaskPlannerClient()
    private var bubble: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var realtimeSession: RealtimeVoiceSession? = null
    private var voicePreparationError: String? = null
    private var latestText = ""
    private val voiceController = VoiceSessionController()
    private val voiceScheduler = object : VoiceSessionScheduler {
        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            mainHandler.postDelayed(action, delayMs)
        }
    }
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var initialX = 0
    private var initialY = 0
    private var listeningByGesture = false
    private var isHolding = false
    private var moved = false
    private var sessionActive = false
    private var pulseAnimator: ValueAnimator? = null

    private val longPress = Runnable {
        if (!moved) {
            val preparationError = voicePreparationError
            if (preparationError != null) {
                showRealtimeError(preparationError)
            } else {
                isHolding = true
                listeningByGesture = true
                startListening()
            }
        }
    }

    private val voiceDurationTimeout = Runnable {
        if (sessionActive && isHolding) {
            Toast.makeText(
                this,
                "已听满 30 秒，正在整理文字",
                Toast.LENGTH_SHORT
            ).show()
            finishListening()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        shizukuBridge = ShizukuUserServiceBridge(this)
        shizukuBridge.connect()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        if (bubble == null) {
            addBubble()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bubble?.removeCallbacks(longPress)
        bubble?.removeCallbacks(voiceDurationTimeout)
        pulseAnimator?.cancel()
        bubble?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubble = null
        realtimeSession?.cancel()
        realtimeSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addBubble() {
        val view = TextView(this).apply {
            text = "搭"
            contentDescription = getString(R.string.bubble_ready)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(Color.WHITE)
            textSize = 24f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(232, 154, 150))
                setStroke(dp(2), Color.rgb(41, 38, 36))
            }
        }

        val params = WindowManager.LayoutParams(
            dp(64),
            dp(64),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dp(84)
            y = resources.displayMetrics.heightPixels / 2
        }

        view.setOnTouchListener { _, event ->
            val currentParams = layoutParams ?: params
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownX = event.rawX
                    touchDownY = event.rawY
                    initialX = currentParams.x
                    initialY = currentParams.y
                    moved = false
                    listeningByGesture = false
                    isHolding = false
                    prepareVoiceSession()
                    view.postDelayed(longPress, LONG_PRESS_MS)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchDownX).toInt()
                    val dy = (event.rawY - touchDownY).toInt()
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) {
                        moved = true
                        view.removeCallbacks(longPress)
                    }
                    if (moved) {
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        windowManager.updateViewLayout(view, currentParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    if (listeningByGesture) {
                        isHolding = false
                        finishListening()
                    } else if (!moved) {
                        realtimeSession?.cancel()
                        realtimeSession = null
                        openMainActivity()
                    } else {
                        realtimeSession?.cancel()
                        realtimeSession = null
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        bubble = view
        layoutParams = params
    }

    private fun startListening() {
        latestText = ""
        sessionActive = true
        voiceController.startPressToTalk()
        bubble?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        showListeningVisual()
        bubble?.postDelayed(voiceDurationTimeout, MAX_RECORDING_MS)
        realtimeSession?.startStreaming() ?: showRealtimeError(
            "麦克风尚未准备好，请重新按住悬浮球。"
        )
    }

    private fun prepareVoiceSession() {
        realtimeSession?.cancel()
        latestText = ""
        voicePreparationError = null
        val session = RealtimeVoiceSession(
            capture = PcmAudioCapture(),
            transport = OkHttpVoiceStream(
                baseUrl = BuildConfig.ORCHESTRATOR_URL,
                deviceToken = BuildConfig.DEVICE_TOKEN,
                deviceId = "xiaozhu-dashou"
            ),
            scheduler = voiceScheduler,
            onPartial = { text ->
                mainHandler.post {
                    if (sessionActive) {
                        latestText = text
                        bubble?.text = text.takeLast(10)
                    }
                }
            },
            onFinal = { text ->
                mainHandler.post {
                    if (sessionActive) {
                        latestText = text
                        submitLatestVoice()
                    }
                }
            },
            onError = { message ->
                mainHandler.post {
                    if (sessionActive) {
                        showRealtimeError(message)
                    }
                }
            }
        )
        realtimeSession = session
        runCatching { session.startCapture() }
            .onFailure {
                realtimeSession = null
                session.cancel()
                voicePreparationError = "无法启动麦克风，请检查录音权限。"
            }
    }

    private fun finishListening() {
        isHolding = false
        bubble?.removeCallbacks(voiceDurationTimeout)
        bubble?.text = getString(R.string.bubble_recognizing)
        realtimeSession?.stopAndFinish() ?: showEmptyResult()
    }

    private fun submitLatestVoice() {
        if (!sessionActive) {
            return
        }
        sessionActive = false
        bubble?.removeCallbacks(voiceDurationTimeout)
        realtimeSession = null
        voiceController.stopPressToTalk(latestText)
        val submitted = voiceController.consumeSubmitted()
        resetBubble()
        if (submitted.isNullOrBlank()) {
            Toast.makeText(
                this,
                R.string.voice_empty,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                getString(R.string.voice_result_prefix) + submitted,
                Toast.LENGTH_LONG
            ).show()
            submitToPlanner(submitted)
        }
    }

    private fun submitToPlanner(instruction: String) {
        Toast.makeText(this, "正在理解任务…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = taskPlannerClient.submit(instruction)
            mainHandler.post {
                when (result) {
                    is PlannerResult.Plan -> executePlannedActions(result)
                    is PlannerResult.Message -> {
                        Toast.makeText(
                            this,
                            result.text,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is PlannerResult.Failure -> {
                        Toast.makeText(
                            this,
                            "任务服务不可用：${result.reason}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    private fun executePlannedActions(plan: PlannerResult.Plan) {
        if (plan.actions.isEmpty()) {
            Toast.makeText(this, "任务没有可执行动作", Toast.LENGTH_SHORT).show()
            return
        }

        val driver = FallbackExecutionDriver(
            ShizukuDriver(shizukuBridge),
            AccessibilityDriver(this)
        )
        val runner = SafeExecutionRunner(
            driver = driver,
            initialGeneration = plan.actions.first().taskGeneration
        )

        fun executeAt(index: Int) {
            if (index >= plan.actions.size) {
                Toast.makeText(
                    this,
                    "任务执行完成，请核对最终页面",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val planned = plan.actions[index]
            val runtimeAction = when (planned.type) {
                "open_app" -> RuntimeAction(
                    actionId = planned.actionId,
                    taskId = planned.taskId,
                    taskGeneration = planned.taskGeneration,
                    type = RuntimeActionType.OPEN_APP,
                    text = null,
                    targetPackage = planned.targetPackage
                )
                "restart_app" -> RuntimeAction(
                    actionId = planned.actionId,
                    taskId = planned.taskId,
                    taskGeneration = planned.taskGeneration,
                    type = RuntimeActionType.RESTART_APP,
                    text = null,
                    targetPackage = planned.targetPackage
                )
                "tap" -> RuntimeAction(
                    actionId = planned.actionId,
                    taskId = planned.taskId,
                    taskGeneration = planned.taskGeneration,
                    type = RuntimeActionType.TAP,
                    text = null,
                    requiredPackage = planned.expectedPackage,
                    targetX = planned.targetX?.toFloat(),
                    targetY = planned.targetY?.toFloat()
                )
                "input_text" -> RuntimeAction(
                    actionId = planned.actionId,
                    taskId = planned.taskId,
                    taskGeneration = planned.taskGeneration,
                    type = RuntimeActionType.INPUT_TEXT,
                    text = planned.text,
                    requiredPackage = planned.expectedPackage
                )
                else -> null
            }

            if (runtimeAction == null) {
                Toast.makeText(
                    this,
                    "暂不支持动作：${planned.type}",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val observation = AccessibilityObserver.instance?.snapshot()
                ?: ObservationSnapshot(
                    foregroundPackage = "unknown",
                    pageTitle = null,
                    pageKind = PageKind.NORMAL,
                    recipientResolution = RecipientResolution.NONE,
                    nodes = emptyList()
                )
            when (val result = runner.execute(runtimeAction, observation)) {
                ActionResult.Completed -> {
                    bubble?.postDelayed(
                        { executeAt(index + 1) },
                        if (runtimeAction.type == RuntimeActionType.OPEN_APP) {
                            OPEN_APP_DELAY_MS
                        } else if (runtimeAction.type == RuntimeActionType.RESTART_APP) {
                            RESTART_APP_DELAY_MS
                        } else {
                            ACTION_DELAY_MS
                        }
                    )
                }
                is ActionResult.Rejected -> {
                    Toast.makeText(this, "任务已取消或过期", Toast.LENGTH_LONG).show()
                }
                is ActionResult.NeedsConfirmation -> {
                    Toast.makeText(this, "需要你确认后继续", Toast.LENGTH_LONG).show()
                }
                is ActionResult.Blocked -> {
                    Toast.makeText(this, "当前页面需要你本人处理", Toast.LENGTH_LONG).show()
                }
                is ActionResult.Failed -> {
                    Toast.makeText(
                        this,
                        "执行失败：${result.reason}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        executeAt(0)
    }

    private fun showEmptyResult() {
        if (!sessionActive) {
            return
        }
        sessionActive = false
        bubble?.removeCallbacks(voiceDurationTimeout)
        realtimeSession?.cancel()
        realtimeSession = null
        voiceController.cancel()
        resetBubble()
        Toast.makeText(
            this,
            R.string.voice_empty,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showRealtimeError(message: String) {
        sessionActive = false
        bubble?.removeCallbacks(voiceDurationTimeout)
        realtimeSession?.cancel()
        realtimeSession = null
        voiceController.cancel()
        resetBubble()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun resetBubble() {
        listeningByGesture = false
        isHolding = false
        bubble?.textSize = 24f
        bubble?.text = "搭"
        bubble?.contentDescription = getString(R.string.bubble_ready)
        updateBubbleWidth(CIRCLE_SIZE_DP)
        pulseAnimator?.cancel()
        pulseAnimator = null
        bubble?.scaleX = 1f
        bubble?.scaleY = 1f
        bubble?.background = bubbleBackground(cornerRadiusDp = 32, strokeDp = 2)
    }

    private fun showListeningVisual() {
        val view = bubble ?: return
        view.text = getString(R.string.bubble_listening_prompt)
        view.textSize = 17f
        updateBubbleWidth(LISTENING_WIDTH_DP)
        view.background = bubbleBackground(cornerRadiusDp = 24, strokeDp = 3)
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.06f).apply {
            duration = 480L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
            }
            start()
        }
    }

    private fun updateBubbleWidth(widthDp: Int) {
        val params = layoutParams ?: return
        val view = bubble ?: return
        params.width = dp(widthDp)
        params.x = min(
            params.x,
            resources.displayMetrics.widthPixels - params.width - dp(16)
        )
        windowManager.updateViewLayout(view, params)
    }

    private fun bubbleBackground(
        cornerRadiusDp: Int,
        strokeDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerRadiusDp).toFloat()
            setColor(Color.rgb(232, 154, 150))
            setStroke(dp(strokeDp), Color.rgb(41, 38, 36))
        }
    }

    private fun openMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.bubble_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.bubble_ready))
            .setContentText("长按悬浮球开始说话")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "xiaozhu_dashou_bubble"
        private const val NOTIFICATION_ID = 2026
        private const val LONG_PRESS_MS = 450L
        private const val MAX_RECORDING_MS = 30_000L
        private const val CIRCLE_SIZE_DP = 64
        private const val LISTENING_WIDTH_DP = 164
        private const val ACTION_DELAY_MS = 1_200L
        private const val OPEN_APP_DELAY_MS = 2_500L
        private const val RESTART_APP_DELAY_MS = 3_500L
    }
}
