package com.github.aakumykov.copy_between_streams_with_counting

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CopyBetweenStreamsWithCountingInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val cacheDir: File get() = appContext.cacheDir

    private val random: Random get() = Random

    private var fileNumber: Int = 0

    private val randomDataChinkSize = 1024
    private var randomDataMultiplier: Int = -1
    private var sourceFileSize: Int = -1

    private val sourceFileName = "source_${fileNumber}.file"
    private lateinit var sourceFile: File
    private lateinit var sourceStream: InputStream

    private val targetFileName = "target_${fileNumber}.file"
    private lateinit var targetFile: File
    private lateinit var targetFileStream: OutputStream


    private fun prepareLateinitProperties() {
        fileNumber = random.nextInt(1, 1000)

        randomDataMultiplier = random.nextInt(1,11)
        sourceFileSize = randomDataMultiplier * randomDataChinkSize

        sourceFile = File(cacheDir, sourceFileName)

        targetFile = File(cacheDir, targetFileName)
        targetFileStream = targetFile.outputStream()
    }


    @Before
    fun prepareTestData() {

        prepareLateinitProperties()
        prepareRandomSourceFileData()

        // Получать поток чтения нужно ПОСЛЕ наполнения файла данными
        //  т.е., фактически, его создания.
        sourceStream = sourceFile.inputStream()
    }

    @After
    fun removeTestData() {
        deleteTestFiles()
        closeFileStreams()
    }


    private fun closeFileStreams() {
        sourceStream.close()
        targetFileStream.close()
    }


    private fun prepareRandomSourceFileData() {
        sourceFile.outputStream().use { outputStream ->
            repeat(randomDataMultiplier) {
                outputStream.write(random.nextBytes(randomDataChinkSize))
            }
        }
    }


    private fun deleteTestFiles() {
        targetFile.delete()
        sourceFile.delete()
    }


    @Test
    fun source_file_exists() {
        assertTrue(sourceFile.exists())
    }


    @Test
    fun source_file_size_equals_declared_size() {
        assertEquals(sourceFile.length(), sourceFileSize.toLong())
    }


    @Test
    fun target_file_exists_after_copying() {
        copyFromSourceToTarget()
        assertTrue(targetFile.exists())
    }


    @Test
    fun target_file_size_equals_source_file_size_after_copying() {
        copyFromSourceToTarget()
        assertEquals(sourceFile.length(), targetFile.length())
    }


    @Test
    fun size_of_source_and_target_files_are_the_same_after_copying() {
        copyFromSourceToTarget()
        assertEquals(sourceFileSize.toLong(), targetFile.length())
    }


    @Test
    fun contents_of_source_and_target_files_are_the_same_after_copying() {

        copyFromSourceToTarget()

        val sourceStream = sourceFile.inputStream()
        val targetStream = targetFile.inputStream()

        val sourceFileByte = ByteArray(1)
        val targetFileByte = ByteArray(1)

        val differences: MutableMap<Int,String> = mutableMapOf()

        repeat(sourceFileSize) { i ->
            sourceStream.read(sourceFileByte)
            targetStream.read(targetFileByte)

            val sourceByte = sourceFileByte.first()
            val targetByte = targetFileByte.first()

            println("$i) sourceByte: $sourceByte, targetByte: $targetByte")

            if (sourceByte != targetByte) {
                differences.put(i,"source_byte: $sourceByte, target_byte: $targetByte")
            }
        }

        if (differences.isNotEmpty()) {

            val diffLog = if (differences.size >= 10) {
                "They has ${differences.size} bytes differs."
            }
            else {
                val diffText = differences.map { entry: Map.Entry<Int,String> ->
                    "${entry.key} ${entry.value}"
                }.joinToString("\n")
                "Differs bytes: $diffText"
            }

            throw Exception("Target file not equals to source. $diffLog")
        }
    }


    @Test
    fun when_copyBetweenStreamsWithCounting_when_callback_methods_are_invoked_right_times() {

        val bufferSize = 1
        var readingCallbackInvokesCount = 0
        var writingCallbackInvokesCount = 0

        copyBetweenStreamsWithCounting(
            inputStream = sourceStream,
            outputStream = targetFileStream,
            bufferSize = bufferSize,
            readingCallback = object: StreamCountingCallbacks.ReadingCallback {
                override fun onReadCountChanged(count: Long) {
                    readingCallbackInvokesCount++
                }
            },
            writingCallback = object: StreamCountingCallbacks.WritingCallback {
                override fun onWriteCountChanged(count: Long) {
                    writingCallbackInvokesCount++
                }
            }
        )

        // Размер буфера 1, поэтому число вызова коллбеков должно равняться размеру файла.
        assertEquals(readingCallbackInvokesCount, sourceFileSize)
        assertEquals(writingCallbackInvokesCount.toLong(), targetFile.length())
    }


    @Test
    fun when_copyBetweenStreamsWithCounting_when_finish_callback_is_invoked() {

        val finishIsInvoked = AtomicBoolean(false)

        copyBetweenStreamsWithCounting(
            inputStream = sourceStream,
            outputStream = targetFileStream,
            finishCallback = { readBytesCount, writeBytesCount ->
                finishIsInvoked.set(true)
            }
        )

        assertTrue(finishIsInvoked.get())
    }


    private fun copyFromSourceToTarget() {
        copyBetweenStreamsWithCounting(
            inputStream = sourceStream,
            outputStream = targetFileStream,
        )
    }


    // Просто я хочу это проверить.
    @Test
    fun files_does_not_exists_alter_deleting_them() {
        deleteTestFiles()
        assertFalse(sourceFile.exists())
        assertFalse(targetFile.exists())
    }
}