package vip.mystery0.pixel.geo.util

import androidx.compose.ui.text.AnnotatedString
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

// iOS 实现：通过 UIActivityViewController 弹出系统分享面板
actual fun shareText(text: String) {
    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    // 获取当前最顶层的 ViewController 并展示分享面板
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(activityViewController, animated = true, completion = null)
}

actual fun copyToClipboard(text: AnnotatedString) {
    val pasteboard = UIPasteboard.generalPasteboard
    pasteboard.string = text.text
}
