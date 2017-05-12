package com.github.sybila.solver

/**
 * Simple solver for enumerated parameter sets represented by standard [Set] objects.
 */
class SetSolver<Item>(
        override val universe: Set<Item>
) : Solver<Set<Item>> {

    override fun Set<Item>?.and(other: Set<Item>?): Set<Item>? = when {
        this == null || other == null -> null
        else -> this.intersect(other).takeIf { it.isNotEmpty() }
    }

    override fun Set<Item>?.or(other: Set<Item>?): Set<Item>? = when {
        this == null -> other
        other == null -> this
        else -> this + other
    }

    override fun Set<Item>?.not(): Set<Item>? = when {
        this == null -> universe
        this == universe -> null
        else -> universe - this
    }

    override fun Set<Item>?.equal(other: Set<Item>?): Boolean = this == other

}