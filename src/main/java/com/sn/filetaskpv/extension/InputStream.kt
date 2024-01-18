package com.sn.filetaskpv.extension

import java.io.InputStream
import java.io.OutputStream

fun InputStream.copyToWithProgress(
    output: OutputStream,
    totalSize: Long,
    callback: (Int) -> Unit
): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead: Int
    var movedSize: Long = 0

    while (read(buffer).also { bytesRead = it } != -1) {
        output.write(buffer, 0, bytesRead)
        movedSize += bytesRead.toLong()
        if (totalSize > 0) {
            val progress = ((movedSize * 100) / totalSize).toInt()
            callback(progress)
        }
    }

    return movedSize
}