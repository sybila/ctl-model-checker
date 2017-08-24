package com.github.sybila.collection

/**
 * A [StateSet] implementation based on standard [Set] interface.
 *
 * It wont break any performance records, but it will get you through the day.
 */
class GenericStateSet<S: Any>(private val data: Set<S>) : StateSet<S> {

    override fun contains(state: S): Boolean = data.contains(state)

    override fun iterator(): Iterator<S> = data.iterator()

    fun copyMutable(): GenericMutableStateSet<S> = GenericMutableStateSet(data)
}