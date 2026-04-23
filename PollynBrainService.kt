package com.stackbleedctrl.pollyn.oslayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stackbleedctrl.pollyn.MainActivity
import com.stackbleedctrl.pollyn.R
import com.stackbleedctrl.pollyn.core.model.PhoneEvent
import com.stackbleedctrl.pollyn.swarm.SwarmCoordinator
import com.stackbleedctrl.pollyn.tracing.PollynTracer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PollynBrainService : Service() {
    @Inject lateinit var bus: BrainEventBus
    @Inject lateinit var brain: PollynBrain
    @Inject lateinit var actions: ActionExecutor
    @Inject lateinit var swarm: SwarmCoordinator
    @Inject lateinit var tracer: PollynTracer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("POLLEN_SERVICE", "PollynBrainService onCreate")
        createChannel()
        startForeground(1001, notification("Pollyn active - starting swarm"))
        swarm.start()
        android.util.Log.d("POLLEN_SERVICE", "swarm.start called")
        bus.events.onEach { event ->
            when (event) {
                is BrainEvent.InputEvent -> handle(event.event)
                is BrainEvent.MeshStatus -> updateNotification(event.text)
                is BrainEvent.DecisionMade -> updateNotification(event.decision.summary)
            }
        }.launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val raw = intent?.getStringExtra(EXTRA_USER_INTENT)
        if (!raw.isNullOrBlank()) bus.tryEmit(BrainEvent.InputEvent(PhoneEvent.UserIntent(raw)))
        return START_STICKY
    }

    override fun onDestroy() {
        swarm.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handle(event: PhoneEvent) {
        scope.launch {
            tracer.trace("brain", "event=${event.summary()}")
            val decision = brain.decide(event)
            actions.execute(decision)
            bus.emit(BrainEvent.DecisionMade(decision))
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Pollyn Brain", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}

val launch = PendingIntent.getActivity(
    this,
    1001,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pollyn")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launch)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(1001, notification(text))
    }

    companion object {
        private const val CHANNEL_ID = "pollyn_brain"
        const val EXTRA_USER_INTENT = "extra_user_intent"
    }
}
