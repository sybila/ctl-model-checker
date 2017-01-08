package com.github.sybila.checker.shared

/**
 * Solver manages parameter set operations.
 *
 * Solver can have internal state, caches, etc. etc.
 * but this has to be implemented in a thread safe manner!
 * (ThreadLocal structures for example)
 */
interface Solver {

    /**
     * Unite [other] parameters with this params set.
     * If nothing changed due to the addition, return null,
     * otherwise return new parameter set.
     */
    fun Params.extendWith(other: Params): Params?

    /**
     * Normal emptiness check.
     */
    fun Params.isSat(): Params?


    /**
     * Semantic comparison operators:
     *
     * A andNot B = exists x \in A: x \not\in B
     * A equals B = x \in A <-> \in B
     *
     * (sometimes can be implemented faster)
     *
     * @Complexity: exponential
     */
    infix fun Params.andNot(other: Params): Boolean = (this and other.not()).isSat() != null
    infix fun Params.semanticEquals(other: Params): Boolean = !((this or other) andNot (this and other))

    infix fun Params.and(other: Params): Params = And(listOf(this, other))
    infix fun Params.or(other: Params): Params = Or(listOf(this, other))
    fun Params.not(): Params = Not(this)

    fun Params.prettyPrint(): String = this.toString()
    fun StateMap.prettyPrint(): String {
        return this.entries.asSequence().map { it.first to it.second.prettyPrint() }.joinToString()
    }

    // Functions for testing

    fun StateMap.deepEquals(other: StateMap): Boolean {
        val states = (this.states + other.states).toSet()
        return states.all {
            //println("Compare ${this[it]} and ${other[it]}")
            (this[it] ?: FF) semanticEquals (other[it] ?: FF)
        }
    }

    fun StateMap.assertDeepEquals(other: StateMap) {
        if (!this.deepEquals(other)) {
            throw IllegalStateException("Expected ${this.prettyPrint()}, but got ${other.prettyPrint()}")
        }
    }


}

class UnsupportedParameterValue(message: Params) : RuntimeException(message.toString())