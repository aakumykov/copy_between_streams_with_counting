package com.github.aakumykov.copy_between_streams_with_counting

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @param inputStream
 * @param outputStream
 * @param bufferSize
 * @param readingCallback По завершении копирования возвращает количество прочитанных байт.
 * @param writingCallback По завершении копирования возвращает количество записанных байт.
 * @return Pair<totalReadBytes:Long,totalWriteBytes:Long>: число прочитанных и записанных байт.
 * В случае, если корутина уже была завершена (cancellableContinuation.isActive == false),
 * в Pair возвращаются значения <-1,-1>
 */
@Throws(IOException::class)
suspend fun copyBetweenStreamsWithCountingSuspend(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: ((totalReadBytes:Long) -> Unit)? = null,
    writingCallback: ((totalWriteBytes:Long) -> Unit)? = null,
)
    : Pair<Long,Long>
{
    return suspendCancellableCoroutine { cancellableContinuation ->

        cancellableContinuation.invokeOnCancellation {
            inputStream.close()
            outputStream.close()
        }

        try {
            // TODO: разобраться с ситуацией, когда isActive == false: что тогда возвращать?
            if (cancellableContinuation.isActive) {
                copyBetweenStreamsWithCounting(
                    inputStream = inputStream,
                    outputStream = outputStream,
                    bufferSize = bufferSize,
                    readingCallback = readingCallback,
                    writingCallback = writingCallback
                ).let {
                    cancellableContinuation.resume(it)
                }
            }
            else {
                cancellableContinuation.resume(Pair(-1,-1))
            }

        } catch (t: Throwable) {
            cancellableContinuation.resumeWithException(t)
        }
    }
}