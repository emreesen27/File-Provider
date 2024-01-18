package com.sn.filetaskpv

import com.sn.filetaskpv.extension.copyToWithProgress
import com.sn.filetaskpv.extension.getMimeType
import com.sn.filetaskpv.extension.getTotalSize
import com.sn.filetaskpv.extension.getUniqueFileNameWithCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.EnumSet

class FileTask {

    private var totalSize: Long = 0
    private var strategy: Pair<FileConflictStrategy, Boolean> =
        Pair(FileConflictStrategy.OVERWRITE, false)


    fun deleteFilesAndDirectories(sourcePaths: List<Path>): Boolean {
        val successList = mutableListOf<Boolean>()
        sourcePaths.forEach { path ->
            val success = path.toFile().deleteRecursively()
            successList.add(success)
        }
        return successList.all { it }
    }

    suspend fun moveFilesAndDirectories(
        sourcePaths: List<Path>,
        destinationPath: Path,
        callback: FileOperationCallback,
        isCopy: Boolean
    ): Result<MutableList<Pair<String, String>>> {
        totalSize = sourcePaths.getTotalSize()
        val moveList: MutableList<Pair<String, String>> = mutableListOf()
        try {
            for (sourcePath in sourcePaths) {
                var targetPath = destinationPath.resolve(sourcePath.fileName)

                if (Files.isDirectory(sourcePath)) {
                    if (Files.exists(targetPath)) {
                        if (!strategy.second) {
                            strategy = callback.fileConflict(sourcePath.toFile())
                        }

                        if (strategy.first == FileConflictStrategy.SKIP) {
                            continue
                        }

                        if (strategy.first == FileConflictStrategy.KEEP_BOTH) {
                            targetPath = targetPath.getUniqueFileNameWithCounter()
                        }
                    }
                    moveDirectoryContents(sourcePath, targetPath, callback, isCopy)
                    if (!isCopy) {
                        deleteFilesAndDirectories(sourcePaths)
                    }

                } else {
                    moveFileWithConflictResolution(sourcePath, targetPath, callback, isCopy)
                }
                moveList.add(Pair(targetPath.toFile().absolutePath, sourcePath.toFile().absolutePath.getMimeType() ?: ""))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return Result.success(moveList)
    }

    private fun moveDirectoryContents(
        sourcePath: Path,
        destinationPath: Path,
        callback: FileOperationCallback,
        isCopy: Boolean
    ) {
        Files.walkFileTree(
            sourcePath,
            EnumSet.noneOf(FileVisitOption::class.java),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val targetFile = destinationPath.resolve(sourcePath.relativize(file))
                    if (Files.exists(targetFile)) {
                        runBlocking {
                            moveFileWithConflictResolution(file, targetFile, callback, isCopy)
                        }
                    } else {
                        startCopyOrMove(file, targetFile, isCopy, callback)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    val targetDir = destinationPath.resolve(sourcePath.relativize(dir))
                    Files.createDirectories(targetDir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
    }

    private suspend fun moveFileWithConflictResolution(
        source: Path,
        target: Path,
        callback: FileOperationCallback,
        isCopy: Boolean
    ) = withContext(Dispatchers.IO) {
        var uniqueTarget = target
        if (Files.exists(target)) {
            if (!strategy.second) {
                strategy = callback.fileConflict(source.toFile())
            }

            if (strategy.first == FileConflictStrategy.SKIP) {
                return@withContext
            }

            if (strategy.first == FileConflictStrategy.KEEP_BOTH) {
                uniqueTarget = target.getUniqueFileNameWithCounter()
            }
        }
        startCopyOrMove(source, uniqueTarget, isCopy, callback)
    }

    private fun startCopyOrMove(
        source: Path,
        target: Path,
        isCopy: Boolean,
        callback: FileOperationCallback
    ) {
        Files.newInputStream(source).use { input ->
            Files.newOutputStream(target).use { output ->
                input.copyToWithProgress(output, totalSize) { progress ->
                    callback.onProgress(progress)
                }
            }
        }
        if (!isCopy) {
            Files.delete(source)
        }
    }
}