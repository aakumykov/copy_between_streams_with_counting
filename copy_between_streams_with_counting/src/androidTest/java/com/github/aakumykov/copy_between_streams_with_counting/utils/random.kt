package com.github.aakumykov.copy_between_streams_with_counting.utils

import java.util.UUID
import kotlin.random.Random

val randomName: String
    get() = UUID.randomUUID().toString()

val randomBytes: ByteArray
    get() = Random.nextBytes(10)

fun randomBytes(amount: Int): ByteArray = Random.nextBytes(amount)

