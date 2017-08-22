package com.github.sybila.collection

/**
 * Create a new [ArrayStateMap] using an [initializer], similar to [Array] constructor
 */
inline fun <reified P: Any> ArrayStateMap(size: Int, noinline initializer: (Int) -> P?): ArrayStateMap<P>
        = ArrayStateMap(Array(size, initializer))