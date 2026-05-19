package com.codewithkael.remotecontrol.models

data class GestureModel(
    val type: GestureType,
    val x: Float, // Normalized 0.0 to 1.0
    val y: Float, // Normalized 0.0 to 1.0
    val xEnd: Float? = null, // Used for DRAG
    val yEnd: Float? = null  // Used for DRAG
)

enum class GestureType {
    CLICK, LONG_CLICK, DRAG
}
