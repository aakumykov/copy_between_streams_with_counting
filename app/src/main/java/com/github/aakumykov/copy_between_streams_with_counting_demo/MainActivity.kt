package com.github.aakumykov.copy_between_streams_with_counting_demo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.aakumykov.copy_between_streams_with_counting.copyBetweenStreamsWithCounting
import com.github.aakumykov.copy_between_streams_with_counting_demo.databinding.ActivityMainBinding
import com.github.aakumykov.copy_between_streams_with_counting_demo.extensions.getStringFromPreferences
import com.github.aakumykov.copy_between_streams_with_counting_demo.extensions.storeStringInPreferences
import com.github.aakumykov.copy_between_streams_with_counting_demo.utils.humanReadableByteCount
import com.github.aakumykov.file_lister_navigator_selector.extensions.errorMsg
import com.github.aakumykov.file_lister_navigator_selector.file_lister.SimpleSortingMode
import com.github.aakumykov.file_lister_navigator_selector.file_selector.FileSelector
import com.github.aakumykov.file_lister_navigator_selector.fs_item.FSItem
import com.github.aakumykov.file_lister_navigator_selector.fs_item.SimpleFSItem
import com.github.aakumykov.local_file_lister_navigator_selector.local_file_selector.LocalFileSelector
import com.github.aakumykov.storage_access_helper.StorageAccessHelper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity(), FileSelector.Callbacks {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storageAccessHelper: StorageAccessHelper
    private val fileSelector: FileSelector<SimpleSortingMode> get() = LocalFileSelector().prepare()

    private var selectedFSItem: FSItem? = null
    private var currentJob: Job? = null

    private val requiredSpeedBytePerSecond: Long get() = 1024 * 1024
    private val perStepSleepMs: Long = 10
    private val stringBuilder: StringBuilder by lazy { StringBuilder() }


    private fun onCopyFileLocallyButtonClicked() {

        val sourceFile = File(selectedFSItem!!.absolutePath)
        val targetFile = File(cacheDir, "output.file")

        val ceh = CoroutineExceptionHandler { _, throwable ->
            showError(throwable)
            hideProgressBar()
            currentJob = null
        }

        currentJob = lifecycleScope.launch {

            showProgressBar(true)
            stringBuilder.clear()

            launch (Dispatchers.IO + ceh) {
                sourceFile.inputStream().use { inputStream ->
                    targetFile.outputStream().use { fileOutputStream ->
                        copyBetweenStreamsWithCounting(
                            inputStream = inputStream,
                            outputStream = fileOutputStream,
                            /*requiredSpeedBytesPerSecond = {
                                requiredSpeedBytePerSecond
                            },*/
                            /*speedCallback = {
                                val humanSpeed = humanReadableByteCount(it.roundToLong())
                                stringBuilder.append(humanSpeed)
                                stringBuilder.append("\n")

//                                launch (Dispatchers.Main) {
//                                    binding.infoView.text = stringBuilder
//                                    binding.infoScrollView.fullScroll(View.FOCUS_DOWN)
//                                }

                                Log.d(TAG, "скорость: $humanSpeed")
                            },*/
                            /*afterWriteCallback = {
                                Thread.sleep(perStepSleepMs)
                            }*/
                        )
                    }
                }
            }.join()

            hideProgressBar()
            currentJob = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        restoreValues()
        prepareButtons()
        prepareComponents()
    }

    private fun restoreValues() {
        selectedFSItem = fsItemFomJSON(getStringFromPreferences(SELECTED_ITEM))
        displayFileSelectionState()
    }

    private fun prepareComponents() {
        storageAccessHelper = StorageAccessHelper.create(this).apply {
            prepareForReadAccess()
        }
    }

    private fun prepareButtons() {
        binding.selectFileButton.setOnClickListener { onSelectFileButtonClicked() }
        binding.copyFileLocallyButton.setOnClickListener { onCopyFileLocallyButtonClicked() }
    }

    private fun onSelectFileButtonClicked() {
        storageAccessHelper.requestReadAccess {
            hideError()
            hideInfo()
            if (storageAccessHelper.hasReadAccess()) selectAFile()
            else storageAccessHelper.requestReadAccess { selectAFile() }
        }
    }

    private fun selectAFile() {
        fileSelector.display(this, this)
    }

    override fun onFileSelected(list: List<FSItem>) {
        selectedFSItem = list.first()
        storeStringInPreferences(SELECTED_ITEM, fsItem2JSON(selectedFSItem))
        displayFileSelectionState()
    }

    private fun displayFileSelectionState() {

        binding.selectFileButton.setText(
            if (null != selectedFSItem) R.string.select_another_file
            else R.string.select_a_file
        )

        if (null != selectedFSItem) {
            val filePath = selectedFSItem!!.absolutePath
            val fileLength = File(filePath).length()
            getString(
                R.string.selected_file_path,
                filePath,
                humanReadableByteCount(fileLength)
            ).also {
                binding.fileInfoView.text = it
            }
        } else {
            binding.fileInfoView.text = getString(R.string.file_not_selected)
        }
    }

    private fun showError(throwable: Throwable) {
        binding.errorView.text = throwable.errorMsg
        Log.d(TAG, throwable.errorMsg, throwable)
    }

    private fun showError(message: String) {
        binding.errorView.text = message
        Log.d(TAG, message)
    }

    private fun hideError() {
        binding.errorView.text = ""
    }

    private fun hideInfo() {
        binding.infoView.text = ""
    }

    private fun showProgressBar(isIndeterminate: Boolean) {
        binding.progressBar.apply {
            this.isIndeterminate = isIndeterminate
            this.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.apply {
            this.isIndeterminate = false
            this.visibility = View.INVISIBLE
        }
    }

    private fun calculateProgress(processed: Long, total: Long): Int {
        if (0L == total) return 0
        return ((1f * processed / total) * 100).roundToInt()
    }

    private fun fsItem2JSON(fsItem: FSItem?): String? {
        return gson.toJson(fsItem)
    }

    private fun fsItemFomJSON(json: String?): FSItem? {
        return gson.fromJson(json, SimpleFSItem::class.java)
    }

    private val gson: Gson by lazy { Gson() }

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        const val SELECTED_ITEM = "SELECTED_ITEM"
    }
}