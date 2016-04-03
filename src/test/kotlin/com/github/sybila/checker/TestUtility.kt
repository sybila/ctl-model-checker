package com.github.sybila.checker

import java.util.*

/**
 * Add here everything you find useful for testing, but can't quite see in the main package
 * (it's unsafe/slow/ugly or just too good for anyone to use!)
 */


/**
 * Return random subset of this set - mainly for testing, etc.
 */
fun <T> Set<T>.randomSubset(): Set<T> {
    val remove = (Math.random() * size).toInt()
    val result = ArrayList<T>(this)
    for (i in 0..remove) {
        val victim = (Math.random() * result.size)
        result.removeAt(victim.toInt())
    }
    return result.toSet()
}

fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1;
    if ( b == 1)        return a;
    if ( b % 2 == 0)    return pow (a * a, b / 2); //even a=(a^2)^b/2
    else                return a * pow (a * a, b / 2); //odd  a=a*(a^2)^b/2
}