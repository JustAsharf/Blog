package com.dailyassistant

import android.app.Application
import com.dailyassistant.data.Repo
import com.dailyassistant.work.Notifications
import com.dailyassistant.work.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyApp : Application() {

    lateinit var repo: Repo
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repo = Repo(this)
        Notifications.createChannels(this)
        appScope.launch {
            repo.seedIfNeeded()
            Scheduler.rescheduleAll(this@DailyApp, repo)
        }
    }
}
