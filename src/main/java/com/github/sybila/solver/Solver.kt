package com.github.sybila.solver

import com.github.sybila.collection.MutableStateMap

/**
 * Solver class provides an implementation for all parameter related operations.
 *
 * It is defined as a standalone object and not as a parameter set interface so that
 * it can maintain an internal state (caches, statistics) without polluting
 * the parameter set class or static context.
 *
 * Implementations of Solver should be always thread safe. If you need to store
 * some thread-sensitive data, we recommend using [ThreadLocal]. If you also need
 * a coordinated shutdown (or data collection) of such values, ensure that the solver
 * is used only on a specific executor and then augment the threads (using [ThreadFactory])
 * to handle the resource shutdown correctly. Or suck it up and use finalizers...
 *
 * For example:
 * ```
 * val solver = ...
 * val executor = Executors.newFixedThreadPool(10) { runnable ->
 *      Thread(Runnable {
 *          try {
 *              solver.openLocal()
 *              runnable.run()
 *          } finally {
 *              solver.closeLocal()
 *          }
 *      })
 * }
 * ```
 *
 * We consider null values to be implicitly valid parameter sets which represent the empty set.
 * Hence we have in general two sets of operations - one, that operates on nullable types
 * and is partially implemented, and a strict one, which assumes non-null values (but not necessarily
 * non-empty).
 *
 * You can still use non-null Param values to represent empty sets (especially when you haven't yet
 * safely determined that the set is empty), but if you can decide some emptiness questions quickly,
 * you can just return null and let the non-strict operators handle the rest.
 */
interface Solver<Param : Any> {

    /** Complete parameter set. */
    val TT: Param

    /** Intersection of the two parameter sets. */
    infix fun Param?.and(with: Param?): Param?
            = if (this == null || with == null) null else this.strictAnd(with)

    /** Union of the two parameter sets. */
    infix fun Param?.or(with: Param?): Param?
            = if (this == null) with else if (with == null) this else this.strictOr(with)

    /** Complement the target parameter set against the given set */
    infix fun Param?.complement(against: Param?): Param?
            = if (this == null || against == null) against else this.strictComplement(against)

    /**
     * Try to compute a union of the two given sets, but return it only if it contains
     * more elements, than the original target set.
     *
     * (This operation requires some kind of emptiness check)
     */
    @Suppress("IfThenToElvis")  // it will screw up the null semantics
    fun Param?.tryOr(with: Param?): Param?
            = if (with == null) null else if (this == null) with else this.strictTryOr(with)

    /**
     * An explicit emptiness check.
     * It gives you an opportunity to also simplify the formula based on the check results,
     * since you don't have to return the same parameter object. */
    fun Param.takeIfNotEmpty(): Param?

    /** @see [and] */
    fun Param.strictAnd(with: Param): Param?

    /** @see [or] */
    fun Param.strictOr(with: Param): Param?

    /** @see [complement] */
    fun Param.strictComplement(against: Param): Param?

    /** @see [tryOr] */
    fun Param.strictTryOr(with: Param): Param?

    /**
     * Increase the parameter set associated with [state] using the [value] set.
     *
     * @return True if the value actually increased, false if no new parameter values were added.
     */
    fun <S: Any> MutableStateMap<S, Param>.increaseKey(state: S, value: Param?): Boolean {
        do {
            val old = this[state]
            val new = old.tryOr(value) ?: return false
        } while (!this.compareAndSet(state, old, new))
        return true
    }

}