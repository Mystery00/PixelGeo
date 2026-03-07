package vip.mystery0.pixel.geo.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

// Kotlin/Native 不支持将动态数组展开传给可变参数 ObjC 方法，改用纯 Kotlin 实现 printf 格式化
actual fun formatString(format: String, vararg args: Any?): String {
    val sb = StringBuilder()
    var i = 0
    var argIndex = 0

    while (i < format.length) {
        if (format[i] != '%') {
            sb.append(format[i++])
            continue
        }
        i++ // 跳过 '%'
        if (i >= format.length) break
        if (format[i] == '%') {
            sb.append('%')
            i++
            continue
        }

        // 解析标志位（目前只处理 '0' 补零标志）
        var zeroPad = false
        if (i < format.length && format[i] == '0') {
            zeroPad = true
            i++
        }

        // 解析宽度
        var widthStr = ""
        while (i < format.length && format[i].isDigit()) {
            widthStr += format[i++]
        }
        val width = widthStr.toIntOrNull() ?: 0

        // 解析精度
        var precision = -1
        if (i < format.length && format[i] == '.') {
            i++
            var precStr = ""
            while (i < format.length && format[i].isDigit()) {
                precStr += format[i++]
            }
            precision = precStr.toIntOrNull() ?: 0
        }

        if (i >= format.length) break
        val conversion = format[i++]
        val arg = if (argIndex < args.size) args[argIndex++] else null

        val formatted = when (conversion) {
            'd', 'i' -> {
                val n = (arg as? Number)?.toLong() ?: arg?.toString()?.toLongOrNull() ?: 0L
                val s = n.toString()
                if (width > 0) (if (zeroPad) s.padStart(width, '0') else s.padStart(width)) else s
            }
            'f', 'F' -> {
                val n = (arg as? Number)?.toDouble() ?: arg?.toString()?.toDoubleOrNull() ?: 0.0
                val prec = if (precision >= 0) precision else 6
                val s = formatDouble(n, prec)
                if (width > 0) (if (zeroPad) s.padStart(width, '0') else s.padStart(width)) else s
            }
            's' -> arg?.toString() ?: ""
            else -> arg?.toString() ?: ""
        }
        sb.append(formatted)
    }

    return sb.toString()
}

private fun formatDouble(value: Double, precision: Int): String {
    if (precision == 0) return round(value).toLong().toString()
    val factor = 10.0.pow(precision)
    val rounded = round(abs(value) * factor).toLong()
    val whole = rounded / factor.toLong()
    val frac = (rounded % factor.toLong()).toString().padStart(precision, '0')
    val sign = if (value < 0) "-" else ""
    return "${sign}${whole}.${frac}"
}
