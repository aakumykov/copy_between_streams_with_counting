package com.github.aakumykov.copy_between_streams_with_counting

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @param inputStream
 * @param outputStream
 * @param requiredSpeedBytesPerSecond Целевая скорость копирования, байт/с. В начале
 * оказывается значительно большей, с копированием каждой порции данных (равной по размеру
 * [bufferSize]) скорость стремится всё ближе к целевой.
 * Будучи Supplier-ом, может быть изменена во время выполнения.
 * @param bufferSize По умолчанию [DEFAULT_BUFFER_SIZE].
 * @param readingCallback По завершении копирования возвращает количество прочитанных байт.
 * @param writingCallback По завершении копирования возвращает количество записанных байт.
 * @param finishCallback Вызывается по завершении копирования, возвращает количество прочитанных
 * и записанных байт.
 * @param speedCallback Вызывается при каждом подсчёте скорости после копирования порции данных,
 * определяемой параметром [bufferSize].
 * @param afterWriteCallback Коллбек, запускаемый после записи каждой порции данных.

 v0.0.10-alpha
 */
@Throws(IOException::class)
fun copyBetweenStreamsWithCounting(
    inputStream: InputStream,
    outputStream: OutputStream,
    requiredSpeedBytesPerSecond: androidx.core.util.Supplier<Long> = androidx.core.util.Supplier { -1 },
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: ((totalReadBytes:Long) -> Unit)? = null,
    writingCallback: ((totalWriteBytes:Long) -> Unit)? = null,
    finishCallback: ((totalReadBytes:Long, totalWriteBytes:Long) -> Unit)? = null,
    speedCallback: ((speedBytesPerSecond: Float) -> Unit)? = null,
    afterWriteCallback: (() -> Unit)? = null
)
    : Pair<Long,Long>
{
    if (bufferSize < 1) {
        throw IllegalArgumentException("Buffer size cannot be smaller then 1")
    }

    var readBytes: Int
    val buffer = ByteArray(bufferSize)

    var totalReadBytes: Long = 0
    var totalWriteBytes: Long = 0

    val startTime = System.currentTimeMillis()

    try {
        while (true) {
            // Чтение из входного потока.
            readBytes = inputStream.read(buffer)

            if (-1 == readBytes) {
                return Pair(totalReadBytes,totalWriteBytes)
            }

            totalReadBytes += readBytes
            readingCallback?.invoke(totalReadBytes)

            // Запись в выходной поток.
            outputStream.write(buffer, 0, readBytes)

            totalWriteBytes += readBytes
            writingCallback?.invoke(totalWriteBytes)

            afterWriteCallback?.invoke()

            // Подсчёт текущей скорости.
            val elapsedMs = System.currentTimeMillis() - startTime
            if (elapsedMs == 0L) {
                continue
            }

            val currentSpeed: Float = readBytes / (elapsedMs / 1024f)
            speedCallback?.invoke(currentSpeed)

            // Подстройка под заданную скорость.
            val targetSpeed = requiredSpeedBytesPerSecond.get()

            if (-1L == targetSpeed) {
                continue
            }

            if (currentSpeed > targetSpeed) {
                val sleepTime = (readBytes * 1024L / targetSpeed) - elapsedMs
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
                }
            }
        }
    } finally {
        finishCallback?.invoke(totalReadBytes, totalWriteBytes)
    }
}