package com.github.sybila.collection

import com.github.sybila.solver.Solver
import java.util.*

/**
 * A counter class keeps a inter -> parameter mapping and provides an increment operation.
 *
 * To function properly, it needs a solver implementation to perform the internal parameter operations.
 *
 * Sadly, I currently don't know how to make this lock-free (but there doesn't seem to be much
 * contention on these locks anyway).
 */
class Counter<P: Any>(private val solver: Solver<P>) {

    private var data: List<P?> = ArrayList<P?>().apply {
        this.add(solver.TT)
    }

    val size: Int
        get() = synchronized(this) { data.size }

    val min: Int
        get() = synchronized(this) { data.indexOfFirst { it != null } }

    val max: Int
        get() = synchronized(this) { data.indexOfLast { it != null } }

    operator fun get(index: Int): P? = synchronized(this) { data.getOrNull(index) }

    fun increment(params: P) = synchronized(this) {
        solver.run {
            val new = ArrayList<P?>()
            for (i in data.indices) {
                addOrUnion(new, i, params complement data[i])
                addOrUnion(new, i+1, data[i] and params)
            }
            //println("Before increment: ${data}")
            data = new.dropLastWhile { it == null }
            //println("After increment: ${data}")
        }
    }

    private fun Solver<P>.addOrUnion(data: ArrayList<P?>, index: Int, params: P?) {
        if (index < data.size) {
            data[index] = (data[index] or params)?.takeIfNotEmpty()
        } else {
            data.add(params?.takeIfNotEmpty())
        }
    }

}