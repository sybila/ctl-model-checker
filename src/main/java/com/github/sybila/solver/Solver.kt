package com.github.sybila.solver

/**
 * Solver class provides an implementation for all parameter related operations.
 *
 * It is defined as a standalone object and not as a parameter set interface so that
 * it can maintain an internal state (caches, statistics) without polluting
 * the parameter set class or static context.
 *
 * Implementations of Solver should be always thread safe.
 */
interface Solver<Param : Any> {

    /**
     * The universe includes all allowed parameter valuations, that is the whole `P`
     */
    val universe: Param

    /**
     * Logical conjunction: `A and B = { x in P | (x in A) and (x in B) }`
     */
    infix fun Param?.and(other: Param?): Param?

    /**
     * Logical disjunction: `A or B = { x in P | (x in A) or (x in B) }`
     */
    infix fun Param?.or(other: Param?): Param?

    /**
     * Logical negation: `A.not() = { x in P | x not in A }`
     */
    fun Param?.not(): Param?

    /**
     * Test for semantic equality. `A equal B = forall (x in P): (x in A) <-> (x in B)`
     */
    infix fun Param?.equal(other: Param?): Boolean

}