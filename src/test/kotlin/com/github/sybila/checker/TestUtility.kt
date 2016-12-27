package com.github.sybila.checker

import com.github.sybila.checker.new.Solver
import com.github.sybila.checker.new.StateMap
import com.github.sybila.checker.new.deepEquals
import kotlin.test.assertTrue

fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1
    if ( b == 1)        return a
    if ( b % 2 == 0)    return pow (a * a, b / 2)       //even a=(a^2)^b/2
    else                return a * pow (a * a, b / 2)   //odd  a=a*(a^2)^b/2
}

fun <Colors> assertDeepEquals(expected: StateMap<Colors>, actual: StateMap<Colors>, solver: Solver<Colors>)
        = assertTrue(deepEquals(expected, actual, solver), "Expected $expected, actual $actual")

fun <Colors> assertDeepEquals(full: Pair<StateMap<Colors>, Solver<Colors>>, partitions: List<Pair<StateMap<Colors>, Solver<Colors>>>)
        = assertTrue(deepEquals(full, partitions), "Expected ${full.first}, actual ${partitions.map { it.first }}")
