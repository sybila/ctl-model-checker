package com.github.sybila.solver

/**
 * Simple solver for binary parameter sets. [Unit] represents true, null false.
 */
class UnitSolver : Solver<Unit> {

    override val universe: Unit = Unit

    override fun Unit?.and(other: Unit?): Unit? = this?.takeIf { other != null }

    override fun Unit?.or(other: Unit?): Unit? = this ?: other

    override fun Unit?.not(): Unit? = Unit.takeIf { this == null }

    override fun Unit?.equal(other: Unit?): Boolean = this == other

}