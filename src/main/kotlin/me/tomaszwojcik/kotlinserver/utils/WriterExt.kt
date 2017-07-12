package me.tomaszwojcik.kotlinserver.utils

import java.io.File
import java.io.Writer
import java.nio.charset.Charset

fun Writer.writeFile(
        file: File,
        charset: Charset = Charsets.UTF_8,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
    val reader = file.reader(charset)
    return reader.use { it.copyTo(this, bufferSize) }
}
