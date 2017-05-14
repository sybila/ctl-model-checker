package com.github.sybila.solver

import java.util.*

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

fun BitSet.copy() = this.clone() as BitSet