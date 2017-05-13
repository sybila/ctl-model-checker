package com.github.sybila.solver

/**
 * A simple class which has only two instances: [True] and [False].
 *
 * We use this class instead of the [Boolean] in order to avoid
 * unnecessary allocations.
 */
sealed class Truth {
    object True : Truth()
    object False : Truth()
}