package com.github.aakumykov.copy_between_streams_with_counting

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @param finishCallback Вызывается по завершении копирования,
 * возвращает количество прочитанных/записанных байт.
 */
@Throws(IOException::class)
fun copyBetweenStreamsWithCounting(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: ((Long) -> Unit)? = null,
    writingCallback: ((Long) -> Unit)? = null,
    finishCallback: ((Long,Long) -> Unit)? = null,
) {
    val dataBuffer = ByteArray(bufferSize)
    var bytesChunk: Int
    var totalReadBytes: Long = 0
    var totalWrittenBytes: Long = 0

    while (true) {
        bytesChunk = inputStream.read(dataBuffer)

        if (-1 == bytesChunk) {
            finishCallback?.invoke(totalReadBytes, totalWrittenBytes)
            return
        }

        totalReadBytes += bytesChunk

        readingCallback?.invoke(totalReadBytes) // FIXME: Long -> Int

        outputStream.write(dataBuffer, 0, bytesChunk)

        totalWrittenBytes += bytesChunk

        writingCallback?.invoke(totalWrittenBytes) // FIXME: Long -> Int
    }
}