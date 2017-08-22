package com.github.sybila.funn

interface Solver<P> {

    // We will add stuff as we go

    val ONE: P
    val ZERO: P

    operator fun P.minus(other: P): P
    operator fun P.plus(other: P): P
    operator fun P.times(other: P): P

    fun P.complement(): P = this.complement(ONE)
    infix fun P.complement(against: P): P = against - this

    // Semantic equality. Standard == tests for syntactic equality
    infix fun P.equal(other: P): Boolean
    infix fun P.notEqual(other: P): Boolean = !(this equal other)

    fun P.isZero(): Boolean = this equal ZERO
    fun P.isNotZero(): Boolean = !this.isZero()

    fun P.isOne(): Boolean = this equal ONE
    fun P.isNotOne(): Boolean = !this.isOne()

    fun <S> AtomicStateMap<S, P>.increaseKey(key: S, value: P): Boolean {
        do {
            val old = this[key]
            val new = old + value
            if (new equal old) return false
        } while (!this.compareAndSet(key, old, new))
        return true
    }

}