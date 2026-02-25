package me.timeto.shared

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ColorRgba(
    val r: Int, val g: Int,
    val b: Int, val a: Int = 255,
) {

    companion object {

        fun fromRgbaStringEx(rgbaString: String): ColorRgba =
            rgbaString.split(',').map { it.toInt() }.let {
                when (it.size) {
                    3 -> ColorRgba(it[0], it[1], it[2])
                    4 -> ColorRgba(it[0], it[1], it[2], it[3])
                    else -> {
                        reportApi("ColorRgba.fromRgbaString($rgbaString) invalid")
                        throw UiException("Invalid color")
                    }
                }
            }

        /**
         * @param h Hue 0..360
         * @param s Saturation 0..100
         * @param l Lightness 0..100
         */
        fun fromHsl(h: Float, s: Float, l: Float, a: Int = 255): ColorRgba {
            val sN = s / 100f
            val lN = l / 100f
            if (sN == 0f) {
                val v = (lN * 255).roundToInt().coerceIn(0, 255)
                return ColorRgba(v, v, v, a)
            }
            val hN = h / 360f
            val q = if (lN < 0.5f) lN * (1f + sN) else lN + sN - lN * sN
            val p = 2f * lN - q
            fun hue2rgb(t0: Float): Int {
                var t = t0
                if (t < 0f) t += 1f
                if (t > 1f) t -= 1f
                val v = when {
                    t < 1f / 6f -> p + (q - p) * 6f * t
                    t < 1f / 2f -> q
                    t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                    else -> p
                }
                return (v * 255).roundToInt().coerceIn(0, 255)
            }
            return ColorRgba(
                r = hue2rgb(hN + 1f / 3f),
                g = hue2rgb(hN),
                b = hue2rgb(hN - 1f / 3f),
                a = a,
            )
        }
    }

    fun toRgbaString(): String =
        "$r,$g,$b,$a"

    /**
     * @return ColorHsl where h: 0..360, s: 0..100, l: 0..100
     */
    fun toHsl(): ColorHsl {
        val rN = r / 255f
        val gN = g / 255f
        val bN = b / 255f
        val maxC = max(rN, max(gN, bN))
        val minC = min(rN, min(gN, bN))
        val l = (maxC + minC) / 2f
        if (maxC == minC)
            return ColorHsl(0f, 0f, l * 100f)
        val d = maxC - minC
        val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
        var h = when (maxC) {
            rN -> (gN - bN) / d + (if (gN < bN) 6f else 0f)
            gN -> (bN - rN) / d + 2f
            else -> (rN - gN) / d + 4f
        }
        h *= 60f
        return ColorHsl(h, s * 100f, l * 100f)
    }
}
