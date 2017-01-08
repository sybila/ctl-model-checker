package com.github.sybila.checker.shared

/**
 * Empty interface that anyone can implement and use
 * to represent parametric sets.
 */
interface Params

/**
 * Logical conjunction of several parametric sets.
 */
data class And(val args: List<Params>) : Params

/**
 * Logical disjunction of several parametric sets.
 */
data class Or(val args: List<Params>) : Params

/**
 * Logical negation (with respect to bounds) of
 * a specific parametric set.
 */
data class Not(val inner: Params) : Params

/**
 * Tautology / Universe
 */
object TT : Params {
    override fun toString(): String = "tt"
}

/**
 * Contradiction / Empty set
 */
object FF : Params {
    override fun toString(): String = "ff"
}