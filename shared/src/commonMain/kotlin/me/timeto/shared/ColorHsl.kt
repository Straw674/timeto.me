package me.timeto.shared

/**
 * @param h Hue 0..360
 * @param s Saturation 0..100
 * @param l Lightness 0..100
 */
data class ColorHsl(
    val h: Float,
    val s: Float,
    val l: Float,
)
