package dev.awd.whistle.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dev.awd.whistle.WhistleApplication
import dev.awd.whistle.managers.StopwatchManager
import dev.awd.whistle.managers.WhistleNotificationManager
import dev.awd.whistle.utils.Constants.ACTION_SERVICE_LAP
import dev.awd.whistle.utils.Constants.ACTION_SERVICE_RESET
import dev.awd.whistle.utils.Constants.ACTION_SERVICE_START
import dev.awd.whistle.utils.Constants.ACTION_SERVICE_STOP


class StopwatchService : Service() {

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    private val binder = StopwatchBinder()

    private val whistleNotificationManager by lazy {
        WhistleNotificationManager(
            context = this@StopwatchService,
            notificationManager = WhistleApplication.notificationModule.notificationManager,
            notificationBuilder = WhistleApplication.notificationModule.notificationBuilder
        )
    }
    private val stopwatchManager = StopwatchManager()


    var time = stopwatchManager.time
        private set
    var currentState = stopwatchManager.stopwatchState
        private set

    var lapsList = stopwatchManager.lapsList
        private set

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action.let {
            when (it) {
                ACTION_SERVICE_START -> {
                    whistleNotificationManager.setStopButton()
                    startForegroundService()
                    stopwatchManager.startStopwatch { minutes, seconds, _ ->
                        whistleNotificationManager.updateNotification(minutes, seconds)
                    }
                }

                ACTION_SERVICE_STOP -> {
                    stopwatchManager.stopStopwatch()
                    whistleNotificationManager.setResumeButton()
                }

                ACTION_SERVICE_RESET -> {
                    stopwatchManager.stopStopwatch()
                    stopwatchManager.resetStopwatch()
                    stopForegroundService()
                }

                ACTION_SERVICE_LAP -> {
                    stopwatchManager.addNewLapTime()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        whistleNotificationManager.setUp { id, builder ->
            startForeground(id, builder)
        }
    }

    private fun stopForegroundService() {
        whistleNotificationManager.cancelNotification {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

}

