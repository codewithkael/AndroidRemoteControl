package com.codewithkael.remotecontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.codewithkael.remotecontrol.models.GestureModel
import org.webrtc.SurfaceViewRenderer

@Composable
fun ObserverView(
    onBack: () -> Unit,
    onObserve: (String) -> Unit,
    isObserving: Boolean,
    onRemoteSurfaceReady: (SurfaceViewRenderer) -> Unit,
    onGesture: (GestureModel) -> Unit,
    shouldShowHint: Boolean,
    onDontShowAgain: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit = {}
) {
    var sharerId by remember { mutableStateOf("") }
    var isFullScreen by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Handle system UI visibility and notify parent
    LaunchedEffect(isFullScreen) {
        onFullScreenChange(isFullScreen)
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { 
                showHintDialog = false
                isFullScreen = true
            },
            title = { Text("Fullscreen Mode") },
            text = { Text("You can go fullscreen to see the remote device better. Use your phone's back button to exit fullscreen mode.") },
            confirmButton = {
                TextButton(onClick = {
                    showHintDialog = false
                    isFullScreen = true
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDontShowAgain()
                    showHintDialog = false
                    isFullScreen = true
                }) {
                    Text("Don't show again")
                }
            }
        )
    }

    if (isFullScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            SurfaceViewRendererComposable(
                modifier = Modifier.fillMaxSize(),
                onSurfaceReady = onRemoteSurfaceReady,
                onGesture = onGesture,
                isFullScreen = true
            )
        }
        
        // Handle back press to exit full screen
        androidx.activity.compose.BackHandler {
            isFullScreen = false
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isObserving) {
                Text(
                    text = "Observe a Screen",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter the ID of the person you want to watch",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = sharerId,
                            onValueChange = { sharerId = it },
                            label = { Text("Sharer ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onObserve(sharerId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Session")
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black, RoundedCornerShape(12.dp))
                ) {
                    SurfaceViewRendererComposable(
                        modifier = Modifier.fillMaxSize(),
                        onSurfaceReady = onRemoteSurfaceReady,
                        onGesture = onGesture
                    )
                    
                    // Small indicator overlay
                    Text(
                        text = "Watching: $sharerId",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color(0x88000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp
                    )

                    // Fullscreen button
                    IconButton(
                        onClick = {
                            if (shouldShowHint) {
                                showHintDialog = true
                            } else {
                                isFullScreen = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color(0x88000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
