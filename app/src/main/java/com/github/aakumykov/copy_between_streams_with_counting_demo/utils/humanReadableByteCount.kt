package com.github.aakumykov.copy_between_streams_with_counting_demo.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun humanReadableByteCount(
    bytes: Long,
    locale: Locale = Locale.getDefault(),
    sizeNames: String = "KMGTPE",
    base: Int = 1024
): String {
    if (bytes < base) return "${bytes}B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val pre = sizeNames[exp - 1]
    return String.format(
        locale,
        "%.1f %ciB",
        bytes / base.toDouble().pow(exp.toDouble()),
        pre
    )
}
