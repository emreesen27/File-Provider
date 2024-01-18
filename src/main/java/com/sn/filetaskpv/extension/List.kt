package com.sn.filetaskpv.extension

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

fun List<Path>.getTotalSize(): Long {
    return this.sumOf { path ->
        if (Files.isRegularFile(path)) Files.size(path)
        else Files.walk(path).filter { Files.isRegularFile(it) }
            .collect(Collectors.summingLong(Files::size))
    }
}