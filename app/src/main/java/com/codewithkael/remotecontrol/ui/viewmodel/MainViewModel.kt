package com.codewithkael.remotecontrol.ui.viewmodel

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codewithkael.remotecontrol.models.GestureModel
import com.codewithkael.remotecontrol.service.CallService
import com.codewithkael.remotecontrol.service.RemoteControlAccessibilityService
import com.codewithkael.remotecontrol.utils.prefs.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private var callService: CallService? = null
    private var isBound = false

    private val _callState = MutableStateFlow(false)
    val callState = _callState.asStateFlow()

    private val _currentRole = MutableStateFlow("NONE")
    val currentRole = _currentRole.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CallService.CallServiceBinder
            callService = binder.getService()
            isBound = true
            observeServiceStates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            callService = null
            isBound = false
        }
    }

    fun initService(context: Context) {
        CallService.startService(context)
        Intent(context, CallService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceStates() {
        viewModelScope.launch {
            callService?.callState?.collectLatest {
                _callState.emit(it)
            }
        }
        viewModelScope.launch {
            callService?.currentRoleState?.collectLatest {
                _currentRole.emit(it)
            }
        }
    }

    fun sendStartCallSignal(participantId: String) {
        callService?.sendStartCallSignal(participantId)
    }

    fun startScreenSharing(intentData: Intent, surface: SurfaceViewRenderer) {
        callService?.startScreenSharing(intentData, surface)
    }

    fun initRemoteSurfaceView(remoteSurface: SurfaceViewRenderer) {
        callService?.initRemoteSurfaceView(remoteSurface)
    }

    fun sendGesture(gesture: GestureModel) {
        callService?.sendGesture(gesture)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/${RemoteControlAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled == 1) {
            val settingValue = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    if (splitter.next().equals(service, ignoreCase = true)) return true
                }
            }
        }
        return false
    }

    fun shouldShowFullScreenHint(): Boolean = preferenceManager.shouldShowFullScreenHint()

    fun setDontShowFullScreenHintAgain() {
        preferenceManager.setDontShowFullScreenHintAgain()
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun stopService(context: Context) {
        CallService.stopService(context)
    }

    fun resetConnection(context: Context) {
        CallService.resetService(context)
    }
}
