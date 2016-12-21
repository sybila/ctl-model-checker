package com.github.sybila.checker.new

import java.nio.ByteBuffer

/**
 * All color related actions are part of the solver.
 *
 * This provides a way to add decoupled caching and
 * other advanced options without complicating the
 * colors interface implementation.
 */
interface Solver<Colors> {

    val tt: Colors
    val ff: Colors

    /**
     * Logical conjunction.
     *
     * Complexity: constant
     */
    infix fun Colors.and(other: Colors): Colors

    /**
     * Logical disjunction.
     *
     * Complexity: constant
     */
    infix fun Colors.or(other: Colors): Colors

    /**
     * Logical negation.
     *
     * Complexity: constant
     */
    fun Colors.not(): Colors

    /**
     * Emptiness check.
     *
     * Complexity: exponential
     */
    fun Colors.isEmpty(): Boolean
    fun Colors.isNotEmpty(): Boolean = !this.isEmpty()

    /**
     * True if A > B (exists e in A: e not in B)
     *
     * Complexity: exponential
     */
    infix fun Colors.andNot(other: Colors): Boolean = this.and(other.not()).isNotEmpty()

    /**
     * Try to reduce the size of the set representation.
     *
     * Complexity: exponential
     */
    fun Colors.minimize(): Colors

    /**
     * Return required number of bytes to serialize this object.
     *
     * Complexity: constant
     */
    fun Colors.byteSize(): Int

    /**
     * Write this object into a byte buffer.
     */
    fun ByteBuffer.putColors(colors: Colors): ByteBuffer
    fun ByteBuffer.getColors(): Colors

    /**
     * Create a copy of this color set with transferred ownership
     */
    fun Colors.transferTo(solver: Solver<Colors>): Colors

}
