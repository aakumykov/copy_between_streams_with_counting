package com.github.aakumykov.copy_between_streams_with_counting

interface StreamCountingCallbacks {

    fun interface ReadingCallback {
        fun onReadCountChanged(count: Long)
    }

    fun interface WritingCallback {
        fun onWriteCountChanged(count: Long)
    }
}