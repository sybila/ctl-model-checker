package com.github.sybila.sharedmem

import com.github.sybila.checker.Solver
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray


class ConcurrentArrayStateMap<P: Any>(
        val capacity: Int, private val solver: Solver<P>
) {

    private val sizeAtomic = AtomicInteger(0)

    val size: Int
        get() = sizeAtomic.get()

    private val data = AtomicReferenceArray<P?>(capacity)

    fun getOrNull(state: Int): P? = data[state]

    fun get(state: Int): P = data[state] ?: solver.ff

    fun union(state: Int, value: P): Boolean {
        solver.run {
            if (value.isNotSat()) return false
            var current: P?
            do {
                current = data[state]
                val c = current ?: ff
                val union = c or value
                if (!union.andNot(c)) return false
            } while (!data.compareAndSet(state, current, union))
            if (current == null) sizeAtomic.incrementAndGet()
            return true
        }
    }

}