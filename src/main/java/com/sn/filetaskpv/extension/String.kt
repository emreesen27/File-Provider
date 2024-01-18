package com.sn.filetaskpv.extension

import android.webkit.MimeTypeMap


fun String.getFileExtension(): String? {
    val lastDotIndex = this.lastIndexOf(".")
    if (lastDotIndex >= 0) {
        return this.substring(lastDotIndex + 1)
    }
    return null
}

fun String.getMimeType(): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(this)
    val ext = if (extension.isNullOrEmpty()) this.getFileExtension() else extension
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
}