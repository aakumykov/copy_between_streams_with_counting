package com.github.aakumykov.copy_between_streams_with_counting.storage_dir

import com.github.aakumykov.copy_between_streams_with_counting.copyBetweenStreamsWithCounting
import com.github.aakumykov.copy_between_streams_with_counting.randomBytes
import com.github.aakumykov.copy_between_streams_with_counting.randomName
import org.junit.Assert
import org.junit.Test

class CopyFromStorageToStorage : TestBase() {

    @Test
    fun from_downloads_to_photo_dir_file_copy() {
        repeat(100) {

            val sFileName = randomName
            val tFileName = randomName
            val data = randomBytes

            val sFile = createFileIn(downloadsDir, sFileName, data)
            val tFile = createFileIn(photosDir, tFileName)

            sFile.inputStream().use { inputStream ->
                tFile.outputStream().use { outputStream ->
                    copyBetweenStreamsWithCounting(inputStream, outputStream)
                }
            }

            // Проверяю, что исходный файл не изменился.
            Assert.assertTrue(sFile.exists())
            Assert.assertEquals(
                sFile.readBytes().joinToString(),
                data.joinToString()
            )

            // Проверяю, что содержимое конечного файла соответствует "исходным" данным.
            Assert.assertTrue(tFile.exists())
            Assert.assertEquals(
                tFile.readBytes().joinToString(),
                data.joinToString()
            )

            // Проверяю, что данные исходного и конечного файлов идентичны.
            Assert.assertEquals(
                sFile.readBytes().joinToString(),
                tFile.readBytes().joinToString()
            )
        }
    }


}