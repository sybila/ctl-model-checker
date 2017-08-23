package com.github.sybila.solver

/**
 * Simple solver for enumerated parameter sets represented by standard [Set] objects.
 */
class SetSolver<Item>(
        override val TT: Set<Item>
) : Solver<Set<Item>> {

    override fun Set<Item>.takeIfNotEmpty(): Set<Item>? = this.takeIf { it.isNotEmpty() }

    override fun Set<Item>.strictAnd(with: Set<Item>): Set<Item>? = this.intersect(with).takeIfNotEmpty()

    override fun Set<Item>.strictOr(with: Set<Item>): Set<Item>? = this.plus(with).takeIfNotEmpty()

    override fun Set<Item>.strictComplement(against: Set<Item>): Set<Item>? = against.minus(this).takeIfNotEmpty()

    override fun Set<Item>.strictTryOr(with: Set<Item>): Set<Item>? = this.plus(with).takeIf { it != this }

}