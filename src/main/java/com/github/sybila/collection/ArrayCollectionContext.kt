package com.github.sybila.collection

import java.util.concurrent.atomic.AtomicReferenceArray

abstract class ArrayCollectionContext<P: Any>(private val capacity: Int) : CollectionContext<Int, P> {

    abstract fun makeArray(size: Int): Array<P?>

    override fun makeEmptyMap(): MutableStateMap<Int, P> = AtomicArrayStateMap<P>(AtomicReferenceArray(capacity))

    override fun makeEmptySet(): MutableStateSet<Int> = AtomicArrayStateSet(IntArray(capacity))

    override fun StateMap<Int, P>.toMutable(): MutableStateMap<Int, P>
            = if (this is ArrayStateMap<P>) this.makeMutable() else {
        val copy = makeArray(capacity)
        this.entries.forEach { (s, p) -> copy[s] = p }
        AtomicArrayStateMap(AtomicReferenceArray(copy))
    }

    override fun StateSet<Int>.toMutable(): MutableStateSet<Int>
            = if (this is ArrayStateSet) this.makeMutable() else {
        AtomicArrayStateSet(IntArray(capacity) { if (it in this) 1 else 0 })
    }

    override fun MutableStateMap<Int, P>.toReadOnly(): StateMap<Int, P> {
        val copy = makeArray(capacity)
        this.entries.forEach { (s, p) -> copy[s] = p }
        return ArrayStateMap(copy)
    }

    override fun MutableStateSet<Int>.toReadOnly(): StateSet<Int>
            = if (this is AtomicArrayStateSet) this.makeReadOnly() else {
        ArrayStateSet(IntArray(capacity) { if (it in this) 1 else 0 })
    }
}