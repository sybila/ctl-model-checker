package com.github.sybila.sharedmem

import java.util.*

typealias StateMap<P> = ConcurrentArrayStateMap<P>

inline fun <T> List<T>.mergePairs(merge: (T, T) -> T): List<T> {
    val result = ArrayList<T>(this.size + 1)
    var i = 0
    while (i+1 < size) {
        result.add(merge(this[i], this[i+1]))
        i += 2
    }
    if (size % 2 == 1) {
        result.add(this.last())
    }
    return result
}

inline fun <P> List<P>.merge(crossinline action: (P, P) -> P): P {
    var items = this
    while (items.size > 1) {
        items = items.mergePairs(action)
    }
    return items[0]
}