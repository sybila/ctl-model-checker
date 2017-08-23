package com.github.sybila.coroutines

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class PackageTest {

    @Test
    fun consumeChunks() {
        runBlocking {
            val data = (0 until 1000).toList()
            val result = IntArray(1000)
            data.consumeChunks({ result[it] = 5 + it }, fork = 4)
            assertEquals((0 until 1000).map { it + 5 }, result.toList())
        }
    }

    @Test
    fun mapChunks() {
        runBlocking {
            val data = (0 until 1000).toList()
            val result = data.mapChunks({ (5 + it).toString() }, fork = 4)
            assertEquals((0 until 1000).map { (it + 5).toString() }, result)
        }
    }

}