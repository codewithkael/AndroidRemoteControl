package com.codewithkael.remotecontrol.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import com.codewithkael.remotecontrol.models.GestureModel
import com.codewithkael.remotecontrol.models.GestureType

class RemoteControlAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun executeGesture(gesture: GestureModel) {
        val displayMetrics = resources.displayMetrics
        val x = gesture.x * displayMetrics.widthPixels
        val y = gesture.y * displayMetrics.heightPixels

        when (gesture.type) {
            GestureType.CLICK -> {
                dispatchClick(x, y)
            }
            GestureType.LONG_CLICK -> {
                dispatchLongClick(x, y)
            }
            GestureType.DRAG -> {
                if (gesture.xEnd != null && gesture.yEnd != null) {
                    val xEnd = gesture.xEnd * displayMetrics.widthPixels
                    val yEnd = gesture.yEnd * displayMetrics.heightPixels
                    dispatchDrag(x, y, xEnd, yEnd)
                }
            }
        }
    }

    private fun dispatchClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun dispatchLongClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun dispatchDrag(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    companion object {
        private var instance: RemoteControlAccessibilityService? = null

        fun getInstance(): RemoteControlAccessibilityService? = instance
    }
}
