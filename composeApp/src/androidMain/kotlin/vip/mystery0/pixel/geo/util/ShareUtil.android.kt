package vip.mystery0.pixel.geo.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.text.AnnotatedString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import vip.mystery0.pixel.geo.R

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

actual fun copyToClipboard(text: AnnotatedString) {
    // 通过 Koin 获取 Application Context，避免持有 Activity 引用
    val context: Context = object : KoinComponent {
        val ctx: Context by inject()
    }.ctx
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(context.getString(R.string.app_name), text)
    clipboardManager.setPrimaryClip(clipData)
}
