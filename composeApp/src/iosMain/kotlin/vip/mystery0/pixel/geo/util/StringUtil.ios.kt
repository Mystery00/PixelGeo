package vip.mystery0.pixel.geo.util

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatString(format: String, vararg args: Any?): String =
    NSString.stringWithFormat(format, *args)
