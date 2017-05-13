package com.github.sybila.solver

/**
 * Solver class provides an implementation for all parameter related operations.
 *
 * It is defined as a standalone object and not as a parameter set interface so that
 * it can maintain an internal state (caches, statistics) without polluting
 * the parameter set class or static context.
 *
 * Implementations of Solver should be always thread safe.
 *
 */
interface Solver<Param : Any> {

    /**
     * The true set includes all allowed parameter valuations, that is, the whole `P`
     */
    val tt: Param

    /**
     * The false set is an empty parameter set which is not satisfiable.
     */
    val ff: Param

    /**
     * Logical conjunction: `A and B = { x in P | (x in A) and (x in B) }`
     */
    infix fun Param.and(other: Param): Param

    /**
     * Logical disjunction: `A or B = { x in P | (x in A) or (x in B) }`
     */
    infix fun Param.or(other: Param): Param

    /**
     * Logical negation: `A.not() = { x in P | x not in A }`
     */
    fun Param.not(): Param

    /**
     * Test for semantic equality. `A equal B = ForAll (x in P): (x in A) <-> (x in B)`
     */
    infix fun Param.equal(other: Param): Boolean

    /**
     * See [equal].
     * @see [equal]
     */
    infix fun Param.notEqual(other: Param): Boolean = !(this equal other)

    /**
     * Simple emptiness test which relies on the fact that only null parameter
     * set is empty.
     */
    fun Param.isSat(): Boolean

    /**
     * See [isSat].
     * @see [isSat]
     */
    fun Param.isNotSat(): Boolean = !this.isSat()

    /**
     * Return disjunction of the two arguments if result is different ("bigger") than
     * the target parameter set. Otherwise return null.
     *
     * One of our "domain specific" operations, used heavily to update mutable state
     * maps.
     *
     * Falls back to standard [or] and [equal] implementations.
     */
    infix fun Param.tryOr(other: Param): Param? = (this or other).takeIf { it notEqual this }

    /**
     * Return conjunction of the two arguments if result is different ("smaller") than
     * the target parameter set. Otherwise return null.
     *
     * One of our "domain specific" operations, used heavily to update mutable state
     * maps.
     *
     * Falls back to standard [and] and [equal] implementations.
     */
    infix fun Param.tryAnd(other: Param): Param? = (this and other).takeIf { it notEqual this }

}