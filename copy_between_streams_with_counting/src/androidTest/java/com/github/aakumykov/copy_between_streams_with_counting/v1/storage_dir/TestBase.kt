package com.github.aakumykov.copy_between_streams_with_counting.v1.storage_dir

import android.os.Environment
import com.github.aakumykov.copy_between_streams_with_counting.helpers.StorageAccessTestCase
import java.io.File

abstract class TestBase : StorageAccessTestCase() {

    protected val storageRootDir: File
        get() = Environment.getExternalStorageDirectory()

    protected val downloadsDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    protected val photosDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

    protected fun createFileIn(
        parentDir: File, fileName: String, fileContents: ByteArray? = null
    ): File {
        return File(parentDir, fileName).apply {
            createNewFile()
            if (null != fileContents)
                writeBytes(fileContents)
        }
    }
}