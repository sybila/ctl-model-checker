package com.github.sybila.algorithm

import com.github.sybila.model.StateMap
import kotlin.test.assertEquals

fun <P : Any> assertMapEquals(expected: StateMap<Int, P>, actual: StateMap<Int, P>, bounds: IntRange) {
    for (s in bounds) {
        assertEquals(expected[s], actual[s], "Error in state $s")
    }
}