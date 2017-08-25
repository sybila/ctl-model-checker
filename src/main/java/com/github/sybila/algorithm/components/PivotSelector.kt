package com.github.sybila.algorithm.components

import com.github.sybila.collection.StateMap

/**
 * Pivot selector is a class used to pick pivots during component detection algorithm.
 */
interface PivotSelector<S: Any, P: Any> {

    /**
     * Select a sub-StateMap of this map as a pivot set. In the resulting set, parameters
     * for each state are pair-wise disjoint and for every valuations in the original map
     * exists a state in the result which contains this valuation.
     */
    fun StateMap<S, P>.findPivots(): StateMap<S, P>

}