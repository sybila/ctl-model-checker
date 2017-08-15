package com.github.sybila.funn

interface Solver<P> {

    // We will add stuff as we go

    val ONE: P
    val ZERO: P

    operator fun P.minus(other: P): P
    operator fun P.plus(other: P): P
    operator fun P.times(other: P): P

    infix fun P.equal(other: P): Boolean

    fun P.isZero(): Boolean = this equal ZERO

    fun P.isOne(): Boolean = this equal ONE

}