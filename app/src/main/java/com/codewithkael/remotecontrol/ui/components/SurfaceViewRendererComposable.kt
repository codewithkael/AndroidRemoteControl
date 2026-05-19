package com.codewithkael.remotecontrol.ui.components

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.codewithkael.remotecontrol.models.GestureModel
import com.codewithkael.remotecontrol.models.GestureType
import org.webrtc.SurfaceViewRenderer
import kotlin.math.sqrt

@Composable
fun SurfaceViewRendererComposable(
    modifier: Modifier = Modifier,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit,
    onGesture: ((GestureModel) -> Unit)? = null,
    isFullScreen: Boolean = false
) {
    val handler = remember { Handler(Looper.getMainLooper()) }
    
    DisposableEffect(Unit) {
        onDispose {
            handler.removeCallbacksAndMessages(null)
        }
    }

    if (isFullScreen) {
        // Immersive Fullscreen - No Card, no padding
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                var startX = 0f
                var startY = 0f
                var isLongClickTriggered = false
                
                val longClickRunnable = Runnable {
                    if (onGesture != null) {
                        isLongClickTriggered = true
                        onGesture(
                            GestureModel(
                                type = GestureType.LONG_CLICK,
                                x = startX / ctx.resources.displayMetrics.widthPixels.toFloat(),
                                y = startY / ctx.resources.displayMetrics.heightPixels.toFloat()
                            )
                        )
                    }
                }

                val view = SurfaceViewRenderer(ctx).apply {
                    onSurfaceReady.invoke(this)
                }
                FrameLayout(ctx).apply {
                    addView(view)
                    setOnTouchListener(object : View.OnTouchListener {
                        override fun onTouch(v: View, event: MotionEvent): Boolean {
                            if (onGesture == null) return false
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    startX = event.x
                                    startY = event.y
                                    isLongClickTriggered = false
                                    handler.postDelayed(longClickRunnable, 500)
                                    return true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    val diffX = event.x - startX
                                    val diffY = event.y - startY
                                    val distance = sqrt(((diffX * diffX) + (diffY * diffY)).toDouble())
                                    if (distance > 30) {
                                        handler.removeCallbacks(longClickRunnable)
                                    }
                                    return true
                                }
                                MotionEvent.ACTION_UP -> {
                                    handler.removeCallbacks(longClickRunnable)
                                    if (!isLongClickTriggered) {
                                        val endX = event.x
                                        val endY = event.y
                                        val diffX = endX - startX
                                        val diffY = endY - startY
                                        val distance = sqrt(((diffX * diffX) + (diffY * diffY)).toDouble())

                                        if (distance < 30) {
                                            // Click
                                            v.performClick()
                                            onGesture(
                                                GestureModel(
                                                    type = GestureType.CLICK,
                                                    x = event.x / v.width,
                                                    y = event.y / v.height
                                                )
                                            )
                                        } else {
                                            // Drag
                                            onGesture(
                                                GestureModel(
                                                    type = GestureType.DRAG,
                                                    x = startX / v.width,
                                                    y = startY / v.height,
                                                    xEnd = endX / v.width,
                                                    yEnd = endY / v.height
                                                )
                                            )
                                        }
                                    }
                                    return true
                                }
                                MotionEvent.ACTION_CANCEL -> {
                                    handler.removeCallbacks(longClickRunnable)
                                    return true
                                }
                                else -> return false
                            }
                        }
                    })
                }
            }
        )
    } else {
        // Normal mode with Card and padding
        Card(
            modifier = modifier
                .padding(8.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SurfaceViewRenderer
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        var startX = 0f
                        var startY = 0f
                        var isLongClickTriggered = false

                        val longClickRunnable = Runnable {
                            if (onGesture != null) {
                                isLongClickTriggered = true
                                // We need to be careful with coordinates here as v.width might not be ready in Runnable immediately 
                                // but ctx is available. Actually v is passed to onTouch, so we can use it there.
                            }
                        }

                        val view = SurfaceViewRenderer(ctx).apply {
                            onSurfaceReady.invoke(this)
                        }
                        FrameLayout(ctx).apply {
                            addView(view)
                            setOnTouchListener(object : View.OnTouchListener {
                                // Redefine runnable to capture the view for width/height
                                private val runnable = Runnable {
                                    isLongClickTriggered = true
                                    onGesture?.invoke(
                                        GestureModel(
                                            type = GestureType.LONG_CLICK,
                                            x = startX / width,
                                            y = startY / height
                                        )
                                    )
                                }

                                override fun onTouch(v: View, event: MotionEvent): Boolean {
                                    if (onGesture == null) return false
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            startX = event.x
                                            startY = event.y
                                            isLongClickTriggered = false
                                            handler.postDelayed(runnable, 500)
                                            return true
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            val diffX = event.x - startX
                                            val diffY = event.y - startY
                                            val distance = sqrt(((diffX * diffX) + (diffY * diffY)).toDouble())
                                            if (distance > 30) {
                                                handler.removeCallbacks(runnable)
                                            }
                                            return true
                                        }
                                        MotionEvent.ACTION_UP -> {
                                            handler.removeCallbacks(runnable)
                                            if (!isLongClickTriggered) {
                                                val endX = event.x
                                                val endY = event.y
                                                val diffX = endX - startX
                                                val diffY = endY - startY
                                                val distance = sqrt(((diffX * diffX) + (diffY * diffY)).toDouble())

                                                if (distance < 30) {
                                                    // Click
                                                    v.performClick()
                                                    onGesture(
                                                        GestureModel(
                                                            type = GestureType.CLICK,
                                                            x = event.x / v.width,
                                                            y = event.y / v.height
                                                        )
                                                    )
                                                } else {
                                                    // Drag
                                                    onGesture(
                                                        GestureModel(
                                                            type = GestureType.DRAG,
                                                            x = startX / v.width,
                                                            y = startY / v.height,
                                                            xEnd = endX / v.width,
                                                            yEnd = endY / v.height
                                                        )
                                                    )
                                                }
                                            }
                                            return true
                                        }
                                        MotionEvent.ACTION_CANCEL -> {
                                            handler.removeCallbacks(runnable)
                                            return true
                                        }
                                        else -> return false
                                    }
                                }
                            })
                        }
                    }
                )
            }
        }
    }
}
