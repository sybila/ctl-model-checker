package com.github.sybila.solver

/**
 * Simple solver for enumerated parameter sets represented by standard [Set] objects.
 */
class SetSolver<Item>(
        override val tt: Set<Item>
) : Solver<Set<Item>> {

    override val ff: Set<Item> = setOf()

    override fun Set<Item>.and(other: Set<Item>): Set<Item> = when {
        this.isEmpty() || other.isEmpty() -> ff
        else -> this.intersect(other)
    }

    override fun Set<Item>.or(other: Set<Item>): Set<Item> = when {
        this.isEmpty() -> other
        other.isEmpty() -> this
        else -> this + other
    }

    override fun Set<Item>.not(): Set<Item> = when {
        this.isEmpty() -> tt
        this == tt -> ff
        else -> tt - this
    }

    override fun Set<Item>.equal(other: Set<Item>): Boolean = this == other

    override fun Set<Item>.isSat(): Boolean = this.isNotEmpty()

}