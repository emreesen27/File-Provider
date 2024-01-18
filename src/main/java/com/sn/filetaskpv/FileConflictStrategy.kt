package com.sn.filetaskpv

enum class FileConflictStrategy(val value: Int) {
    SKIP(0), KEEP_BOTH(1), OVERWRITE(2);

    companion object {
        fun getByValue(value: Int): FileConflictStrategy? {
            return values().find { it.value == value }
        }
    }
}