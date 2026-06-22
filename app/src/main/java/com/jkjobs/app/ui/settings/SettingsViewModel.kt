package com.jkjobs.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jkjobs.app.data.AlertFrequency
import com.jkjobs.app.data.SettingsRepository
import com.jkjobs.app.worker.JobCheckWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val notificationsEnabled: StateFlow<Boolean> =
        repo.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val frequency: StateFlow<AlertFrequency> =
        repo.frequency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertFrequency.EVERY_3_HOURS)

    /**
     * Toggling does NOT cancel the background worker - it keeps refreshing the cached feed
     * silently (see JobCheckWorker, which checks this same flag before calling Notifier).
     * This means Feed/Saved stay up to date even with alerts muted. If you'd rather fully stop
     * background work when disabled, replace the persistence-only call below with
     * WorkManager.getInstance(app).cancelUniqueWork("job_check_periodic").
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    }

    /** Changing frequency persists the choice AND immediately reschedules WorkManager's
     *  periodic request with the new interval - no app restart required. */
    fun setFrequency(freq: AlertFrequency) {
        viewModelScope.launch {
            repo.setFrequency(freq)
            JobCheckWorker.updateInterval(getApplication(), freq.hours)
        }
    }
}
