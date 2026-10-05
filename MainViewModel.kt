package com.timecapsule.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Prefs
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.Timeline
import com.timecapsule.app.data.supabase
import com.timecapsule.app.notify.Sync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val session = supabase.auth.sessionStatus

    private val _capsules = MutableStateFlow<List<Capsule>>(emptyList())
    val capsules = _capsules.asStateFlow()
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    val timeline = MutableStateFlow(Prefs.loadTimeline(app))
    val timelineLoading = MutableStateFlow(false)

    fun refresh() = viewModelScope.launch {
        loading.value = true
        runCatching { Repo.feed() }
            .onSuccess { _capsules.value = it; Sync.apply(getApplication(), it, notifyNew = false) }
            .onFailure { message.value = Repo.friendly(it) }
        loading.value = false
    }

    fun buildTimeline() = viewModelScope.launch {
        timelineLoading.value = true
        runCatching { Repo.aiTimeline() }
            .onSuccess { timeline.value = it; Prefs.saveTimeline(getApplication(), it) }
            .onFailure { message.value = "No se pudo crear la línea de tiempo: " + Repo.friendly(it) }
        timelineLoading.value = false
    }

    fun signOut() = viewModelScope.launch {
        runCatching { Repo.signOut() }
        Prefs.clear(getApplication())
        _capsules.value = emptyList()
        timeline.value = null
    }
}
