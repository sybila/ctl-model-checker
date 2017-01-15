package com.github.sybila.checker

import com.github.daemontus.Option

/**
 * Solver manages parameter set operations.
 *
 * Solver can have internal state, caches, etc. etc.
 * but this has to be implemented in a thread safe manner!
 */
interface Solver {

    /**
     * Unite [other] parameters with this params set.
     *
     * If resulting parameters contain something more (result andNot this), return them.
     * If other subset this, return None.
     *
     * (the returned parameters can be also optimized in some way)
     */
    fun Params?.extendWith(other: Params?): Option<Params>

    /**
     * Classic emptiness check.
     *
     * Return null if this is not sat, otherwise return new optimized params object.
     */
    fun Params.isSat(): Params?

    /**
     * Semantic comparison operators:
     *
     * A andNot B = exists x \in A: x \not\in B
     * A equals B = x \in A <-> \in B
     *
     * (these are used mainly for testing, overriding them won't speed up the model checker)
     *
     */
    infix fun Params?.andNot(other: Params?): Boolean = (this and other.not())?.isSat() != null
    infix fun Params?.semanticEquals(other: Params?): Boolean = !((this or other) andNot (this and other))

    /**
     * Use these functions to override default toString implementation of logical operators.
     *
     * (default toString should be equivalent to SMT lib 2)
     */
    fun Params?.prettyPrint(): String = this?.let(Params::toString) ?: "(false)"
    fun StateMap.prettyPrint(): String = this.entries
            .map { "(${it.first} ${it.second.prettyPrint()})" }
            .joinToString(prefix = "(map ", postfix = ")")

    // Functions for testing

    fun StateMap.semanticEquals(other: StateMap): Boolean {
        val states = (this.states + other.states).toSet()
        return states.all {
            this[it] semanticEquals other[it]
        }
    }

    fun StateMap.assertDeepEquals(other: StateMap) {
        if (!this.semanticEquals(other)) {
            throw IllegalStateException("Expected ${this.prettyPrint()}, but got ${other.prettyPrint()}")
        }
    }


}

/**
 * Throw this exception if your solver encounters a parameter value which it does not recognize.
 *
 * In general, you should support [And], [Or], [Not], [TT], null (False) and whatever you use as propositions.
 */
class UnsupportedParameterType(message: Params) : RuntimeException(message.toString())