package com.oxygenupdater.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log
import com.oxygenupdater.BuildConfig

fun logVerbose(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.v(tag.ensureLength(), message, cause) else Log.v(tag.ensureLength(), message)
}

fun logDebug(tag: String, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag.ensureLength(), message)
}

fun logInfo(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.i(tag.ensureLength(), message, cause) else Log.i(tag.ensureLength(), message)
}

private fun String.ensureLength() = if (SDK_INT >= VERSION_CODES.O || length <= 23) this else substring(0, 23)
