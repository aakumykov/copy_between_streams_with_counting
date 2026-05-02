package com.github.aakumykov.copy_between_streams_with_counting

import android.util.Log
import androidx.core.util.Supplier
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.concurrent.timerTask

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
 */
@Throws(IOException::class)
fun copyBetweenStreamsWithCounting(
    inputStream: InputStream,
    outputStream: OutputStream,
    requiredSpeedBytesPerSecond: Supplier<Long> = Supplier { -1 },
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

    val incrementalBufferSizes: MutableSet<Int> = buildList<Int> {
        var size: Int = bufferSize
        while (size >= 1) {
            add(size)
            size /= 2
        }
    }.toMutableSet()

    fun currentBufferSize(): Int {
        return if(incrementalBufferSizes.isEmpty()) bufferSize
        else incrementalBufferSizes.let {
            val b = it.last()
            it.remove(b)
            b
        }
    }

    var currentSpeed = 0f

    val speedPublishingTimer: Timer? = if (null != speedCallback) {
        timer(initialDelay = 0L, period = 1000L) {
            Log.d("TIMER", "действие, ${Date().time}")
            speedCallback.invoke(currentSpeed)
        }
    } else null

    try {
        while (true) {
            // Чтение из входного потока.
            readBytes = inputStream.read(buffer, 0, currentBufferSize())

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

            currentSpeed = readBytes / (elapsedMs / 1024f)

            // Подстройка под заданную скорость.
            val targetSpeed = requiredSpeedBytesPerSecond.get()

            if (-1L == targetSpeed) {
                continue
            }

            if (currentSpeed > targetSpeed) {
                val sleepTime = (readBytes * 1024L / targetSpeed) - elapsedMs
                if (sleepTime > 0) {
                    println("sleeping for $sleepTime ms")
                    Thread.sleep(sleepTime)
                }
            }
        }
    } finally {
        speedPublishingTimer?.cancel()
        finishCallback?.invoke(totalReadBytes, totalWriteBytes)
    }
}