package vip.mystery0.pixel.geo.util

import androidx.compose.ui.text.AnnotatedString

// 调用系统分享面板分享文本，由各平台分别实现
expect fun shareText(text: String)

expect fun copyToClipboard(text: AnnotatedString)