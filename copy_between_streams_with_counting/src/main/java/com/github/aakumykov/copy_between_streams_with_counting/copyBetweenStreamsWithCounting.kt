package com.github.aakumykov.copy_between_streams_with_counting

import com.github.aakumykov.copy_between_streams_with_counting.StreamCountingCallbacks
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Throws(IOException::class)
fun copyBetweenStreamsWithCounting(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: StreamCountingCallbacks.ReadingCallback? = null,
    writingCallback: StreamCountingCallbacks.WritingCallback? = null,
) {
    val dataBuffer = ByteArray(bufferSize)
    var readBytes: Int
    var totalBytes: Long = 0

    while (true) {
        readBytes = inputStream.read(dataBuffer)

        if (-1 == readBytes)
            return

        totalBytes += readBytes

        readingCallback?.onReadCountChanged(totalBytes) // FIXME: Long -> Int

        outputStream.write(dataBuffer, 0, readBytes)

        writingCallback?.onWriteCountChanged(totalBytes) // FIXME: Long -> Int
    }
}