package com.github.sybila.checker

import com.github.sybila.huctl.DirectionFormula

/**
 * Represents one transition to/from a state (exact semantics depend on the
 * method returning this value)
 */
data class Transition(
        val target: Int,
        val direction: DirectionFormula.Atom,
        val bound: Params
)