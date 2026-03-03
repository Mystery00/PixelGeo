package vip.mystery0.pixel.geo.util

import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Android 实现：通过系统 Intent.ACTION_SEND 弹出分享面板
actual fun shareText(text: String) {
    // 通过 Koin 获取 Application Context，避免持有 Activity 引用
    val context: Context = object : KoinComponent {
        val ctx: Context by inject()
    }.ctx

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
