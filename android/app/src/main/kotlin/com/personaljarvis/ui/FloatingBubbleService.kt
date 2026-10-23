package com.personaljarvis.ui

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
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.personaljarvis.MainActivity
import com.personaljarvis.R
import com.personaljarvis.voice.VoiceSessionController
import kotlin.math.abs

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var latestText = ""
    private val voiceController = VoiceSessionController()
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var initialX = 0
    private var initialY = 0
    private var listeningByGesture = false
    private var moved = false

    private val longPress = Runnable {
        if (!moved) {
            listeningByGesture = true
            startListening()
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
        voiceController.startPressToTalk()
        bubble?.text = "听"
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
                }
                override fun onResults(results: Bundle?) {
                    latestText = firstResult(results)
                    submitLatestVoice()
                }
                override fun onError(error: Int) {
                    voiceController.cancel()
                    resetBubble()
                    Toast.makeText(
                        this@FloatingBubbleService,
                        R.string.voice_empty,
                        Toast.LENGTH_SHORT
                    ).show()
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
                }
            )
        }
    }

    private fun finishListening() {
        speechRecognizer?.stopListening()
        if (latestText.isNotBlank()) {
            submitLatestVoice()
        }
    }

    private fun submitLatestVoice() {
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

    private fun resetBubble() {
        listeningByGesture = false
        bubble?.text = "搭"
        bubble?.contentDescription = getString(R.string.bubble_ready)
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
    }
}
