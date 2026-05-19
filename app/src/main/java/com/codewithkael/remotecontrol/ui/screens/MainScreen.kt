package com.codewithkael.remotecontrol.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.codewithkael.remotecontrol.ui.components.FooterSection
import com.codewithkael.remotecontrol.ui.components.ObserverView
import com.codewithkael.remotecontrol.ui.components.RoleSelectionSection
import com.codewithkael.remotecontrol.ui.components.SharerView
import com.codewithkael.remotecontrol.ui.components.SurfaceViewRendererComposable
import com.codewithkael.remotecontrol.ui.viewmodel.MainViewModel

@Composable
fun MainScreen() {

    val viewModel: MainViewModel = hiltViewModel()
    val callState by viewModel.callState.collectAsState()
    val roleFromService by viewModel.currentRole.collectAsState()
    val context = LocalContext.current

    var currentRole by remember { mutableStateOf("NONE") } // NONE, SHARER, OBSERVER
    var isObserverFullScreen by remember { mutableStateOf(false) }
    var screenCaptureIntent by remember { mutableStateOf<android.content.Intent?>(null) }

    // Sync currentRole with service state on start
    LaunchedEffect(roleFromService) {
        if (roleFromService != "NONE") {
            currentRole = roleFromService
        }
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            screenCaptureIntent = result.data
            currentRole = "SHARER"
        } else {
            Toast.makeText(context, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            currentRole = "NONE"
        }
    }

    // ---------- Permissions ----------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(
                context, "Microphone and Notification permissions are required", Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.initService(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unbindService(context)
        }
    }

    // Return to main screen if session disconnected while sharing
    LaunchedEffect(callState) {
        if (!callState && currentRole == "SHARER") {
            // Check if we were already in a session and it ended
            // If screenCaptureIntent is null, it means we haven't started yet or just reset
            if (screenCaptureIntent != null) {
                currentRole = "NONE"
                screenCaptureIntent = null
                viewModel.resetConnection(context)
            }
        }
    }

    BackHandler(enabled = currentRole != "NONE") {
        currentRole = "NONE"
        screenCaptureIntent = null
        viewModel.resetConnection(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEAEA))
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentRole) {
                "NONE" -> {
                    RoleSelectionSection(onRoleSelected = { role ->
                        if (role == "SHARER") {
                            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        } else {
                            currentRole = role
                        }
                    })
                }
                "SHARER" -> {
                    SharerView(
                        onBack = { 
                            currentRole = "NONE"
                            screenCaptureIntent = null
                            viewModel.resetConnection(context)
                        },
                        onStopSharing = {
                            currentRole = "NONE"
                            screenCaptureIntent = null
                            viewModel.resetConnection(context)
                        },
                        isSharing = callState,
                        checkAccessibility = { viewModel.isAccessibilityServiceEnabled(context) }
                    )
                }
                "OBSERVER" -> {
                    ObserverView(
                        onBack = { 
                            currentRole = "NONE"
                            viewModel.resetConnection(context)
                        },
                        onObserve = { targetId ->
                            viewModel.sendStartCallSignal(targetId)
                        },
                        isObserving = callState,
                        onRemoteSurfaceReady = { viewModel.initRemoteSurfaceView(it) },
                        onGesture = { viewModel.sendGesture(it) },
                        shouldShowHint = viewModel.shouldShowFullScreenHint(),
                        onDontShowAgain = { viewModel.setDontShowFullScreenHintAgain() },
                        onFullScreenChange = { isObserverFullScreen = it }
                    )
                }
            }
        }

        if (!isObserverFullScreen) {
            FooterSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 10.dp)
            )
        }
    }

    // Handle SurfaceView for Sharer off-screen or invisible if needed by WebRTC
    if (currentRole == "SHARER" && screenCaptureIntent != null) {
        Box(modifier = Modifier.size(1.dp).background(Color.Transparent)) {
            SurfaceViewRendererComposable(
                modifier = Modifier.fillMaxSize(),
                onSurfaceReady = { surface ->
                    screenCaptureIntent?.let { intent ->
                        viewModel.startScreenSharing(intent, surface)
                    }
                }
            )
        }
    }
}
