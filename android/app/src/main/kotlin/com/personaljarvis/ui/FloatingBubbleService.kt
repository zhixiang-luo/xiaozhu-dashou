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
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import com.personaljarvis.MainActivity
import com.personaljarvis.R
import com.personaljarvis.voice.VoiceSessionController
import com.personaljarvis.voice.VoiceErrorAction
import com.personaljarvis.voice.VoiceRecognitionPolicy
import kotlin.math.min
import kotlin.math.abs

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var latestText = ""
    private val voiceController = VoiceSessionController()
    private val recognitionPolicy = VoiceRecognitionPolicy()
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
            isHolding = true
            listeningByGesture = true
            startListening()
        }
    }

    private val releaseTimeout = Runnable {
        if (sessionActive) {
            showEmptyResult()
        }
    }

    private val restartRecognition = Runnable {
        if (isHolding && sessionActive) {
            createAndStartRecognizer()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
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
        bubble?.removeCallbacks(releaseTimeout)
        bubble?.removeCallbacks(restartRecognition)
        pulseAnimator?.cancel()
        bubble?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubble = null
        speechRecognizer?.destroy()
        speechRecognizer = null
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
                        openMainActivity()
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
        createAndStartRecognizer()
    }

    private fun createAndStartRecognizer() {
        bubble?.contentDescription = getString(R.string.bubble_listening)

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onPartialResults(partialResults: Bundle?) {
                    latestText = firstResult(partialResults)
                    if (latestText.isNotBlank() && isHolding) {
                        bubble?.text = latestText.takeLast(10)
                    }
                }
                override fun onResults(results: Bundle?) {
                    latestText = firstResult(results)
                    if (!isHolding) {
                        submitLatestVoice()
                    }
                }
                override fun onError(error: Int) {
                    when (
                        recognitionPolicy.onError(
                            isHolding = isHolding,
                            hasText = latestText.isNotBlank()
                        )
                    ) {
                        VoiceErrorAction.Retry -> {
                            bubble?.postDelayed(restartRecognition, 350)
                        }
                        VoiceErrorAction.SubmitText -> submitLatestVoice()
                        VoiceErrorAction.ShowEmpty -> showEmptyResult()
                    }
                }
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        5_000L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        5_000L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        5_000L
                    )
                }
            )
        }
    }

    private fun finishListening() {
        isHolding = false
        bubble?.removeCallbacks(restartRecognition)
        speechRecognizer?.stopListening()
        if (latestText.isNotBlank()) {
            submitLatestVoice()
        } else {
            bubble?.text = getString(R.string.bubble_recognizing)
            bubble?.postDelayed(releaseTimeout, FINAL_RESULT_GRACE_MS)
        }
    }

    private fun submitLatestVoice() {
        if (!sessionActive) {
            return
        }
        sessionActive = false
        bubble?.removeCallbacks(releaseTimeout)
        bubble?.removeCallbacks(restartRecognition)
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
        }
    }

    private fun showEmptyResult() {
        if (!sessionActive) {
            return
        }
        sessionActive = false
        voiceController.cancel()
        resetBubble()
        Toast.makeText(
            this,
            R.string.voice_empty,
            Toast.LENGTH_SHORT
        ).show()
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

    private fun firstResult(bundle: Bundle?): String {
        return bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
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
        private const val FINAL_RESULT_GRACE_MS = 1_600L
        private const val CIRCLE_SIZE_DP = 64
        private const val LISTENING_WIDTH_DP = 164
    }
}
