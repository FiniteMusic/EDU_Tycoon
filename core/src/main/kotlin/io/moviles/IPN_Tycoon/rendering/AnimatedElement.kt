package io.moviles.IPN_Tycoon.rendering

data class AnimatedElement(
    val animation: BuildingAnimation,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val width: Float,
    val height: Float
)
