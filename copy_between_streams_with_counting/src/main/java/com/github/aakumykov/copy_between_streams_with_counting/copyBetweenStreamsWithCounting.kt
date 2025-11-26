package com.github.aakumykov.copy_between_streams_with_counting

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @param inputStream
 * @param outputStream
 * @param bufferSize
 * @param readingCallback По завершении копирования возвращает количество прочитанных байт.
 * @param writingCallback По завершении копирования возвращает количество записанных байт.
 * @param finishCallback Вызывается по завершении копирования, возвращает количество прочитанных и записанных байт.
 */
@Throws(IOException::class)
fun copyBetweenStreamsWithCounting(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: ((totalReadBytes:Long) -> Unit)? = null,
    writingCallback: ((totalWriteBytes:Long) -> Unit)? = null,
    finishCallback: ((totalReadBytes:Long, totalWriteBytes:Long) -> Unit)? = null,
)
    : Pair<Long,Long>
{
    fun closeStreams() {
        inputStream.close()
        outputStream.close()
    }

    var readBytes: Int
    val buffer = ByteArray(bufferSize)

    var totalReadBytes: Long = 0
    var totalWriteBytes: Long = 0

    try {
        while (true) {
            readBytes = inputStream.read(buffer)

            if (-1 == readBytes) {
                return Pair(totalReadBytes,totalWriteBytes)
            }

            totalReadBytes += readBytes
            readingCallback?.invoke(totalReadBytes)

            outputStream.write(buffer, 0, readBytes)

            totalWriteBytes += readBytes
            writingCallback?.invoke(totalWriteBytes)
        }
    } finally {
        closeStreams()
        finishCallback?.invoke(totalReadBytes, totalWriteBytes)
    }
}