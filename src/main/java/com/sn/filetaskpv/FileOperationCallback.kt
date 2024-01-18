package com.sn.filetaskpv

import java.io.File

interface FileOperationCallback {

    /** Triggered as the copy or move operation continues. Returns the result as a percentage */
    fun onProgress(progress: Int)

    /** Triggered if an conflict occurs while moving or copying files*/
    suspend fun fileConflict(file: File): Pair<FileConflictStrategy, Boolean>

}