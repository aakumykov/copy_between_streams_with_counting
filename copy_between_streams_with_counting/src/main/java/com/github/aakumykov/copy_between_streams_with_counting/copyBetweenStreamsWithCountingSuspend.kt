package com.github.aakumykov.copy_between_streams_with_counting

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume

/**
 * @param inputStream
 * @param outputStream
 * @param bufferSize
 * @param readingCallback По завершении копирования возвращает количество прочитанных байт.
 * @param writingCallback По завершении копирования возвращает количество записанных байт.
 * @return Pair<Long,Long>: число прочитанных и записанных байт.
 */
// TODO: (возможно) StreamCancellationException::class
@Throws(IOException::class)
suspend fun copyBetweenStreamsWithCountingSuspend(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: ((Long) -> Unit)? = null,
    writingCallback: ((Long) -> Unit)? = null,
): Pair<Long,Long> {
    return suspendCancellableCoroutine<Pair<Long,Long>> { cancelableContinuation: CancellableContinuation<Pair<Long,Long>> ->

        var isActive = cancelableContinuation.isActive

        cancelableContinuation.invokeOnCancellation {  cause: Throwable? ->
            isActive = false
            inputStream.close()
            outputStream.close()
        }

        val dataBuffer = ByteArray(bufferSize)
        var readPortionOfBytes: Int
        var totalReadBytes: Long = 0
        var totalWriteBytes: Long = 0

        while (true) {

            if (!isActive)
                break

            readPortionOfBytes = inputStream.read(dataBuffer)
            if (-1 == readPortionOfBytes) {
                cancelableContinuation.resume(Pair(totalReadBytes, totalWriteBytes))
                break
            }
            totalReadBytes += readPortionOfBytes
            readingCallback?.invoke(totalReadBytes)

            outputStream.write(dataBuffer, 0, readPortionOfBytes)
            totalWriteBytes += readPortionOfBytes
            writingCallback?.invoke(totalReadBytes)
        }
    }
}