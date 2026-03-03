package vip.mystery0.pixel.geo.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

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
