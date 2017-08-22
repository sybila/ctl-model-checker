package com.github.sybila.funn

import java.util.*

/**
 * A state queue which uses a non-parametric distance to process states.
 * Non–parametric means that the state is assigned the smallest distance among
 * all parameter valuations.
 *
 * Hence the distance can decrease over time, and it is the responsibility
 * of the user of this queue to propagate this change. This is usually in line with
 * what the parametric algorithm is doing anyway (the distance can change only if
 * a new path is discovered but if a new path is discovered, the information about parameter
 * change needs to be propagated).
 *
 * TierStateQueue is not thread safe!
 */
class TierQueue<State:Any>(model: TransitionSystem<State, *>) : Iterable<Set<Pair<State, State?>>> {

    private val distance = model.genericMap(Int.MAX_VALUE)
    private val tiers: MutableList<MutableSet<Pair<State, State?>>?> = ArrayList()

    private fun getTier(distance: Int): MutableSet<Pair<State, State?>> {
        while (tiers.size <= distance) tiers.add(null)
        return tiers[distance] ?: run {
            val newTier = HashSet<Pair<State, State?>>()
            tiers[distance] = newTier
            newTier
        }
    }

    /**
     * Enqueue given [item], updating it's distance based on [from]. If [from] is null,
     * [item] distance is updated to 0.
     *
     * Amortized complexity of this method is constant.
     */
    fun add(item: State, from: State?) {
        val previousDistance = distance[item] ?: Int.MAX_VALUE
        // If the search is performed correctly, distance[from] should be well defined at this point!
        // Also note that the distance can't reach Int.MAX_VALUE since there are only so many states.
        val newDistance = if (from == null) 0 else {
            if (distance[from] == Int.MAX_VALUE) {
                throw IllegalStateException("Search strategy broken. $item discovered from $from, but $from is not discovered yet.")
            }
            Math.min(distance[item] ?: Int.MAX_VALUE, (distance[from] ?: Int.MAX_VALUE) + 1)
        }
        distance.lazySet(item, newDistance)
        val insert = item to from
        // At this point either:
        // - distance is the same and item is in the correct tier  (1)
        // - distance is the same but item is nowhere in tiers     (2)
        // - distance decreased and item is in the wrong tier      (3)
        // - distance decreased and item is nowhere in tiers       (4)
        if (
                previousDistance > newDistance                      // (3), (4)
                || tiers[newDistance]?.contains(insert) != true   // (2)
                ) {
            // remove item from old tier if it is still present    (3)
            if (previousDistance != Int.MAX_VALUE) {
                // Tier must have existed, but it might have been processed already, in which case all is well.
                // We don't use getTier because we don't want to create useless set if it is not there any more.
                tiers[previousDistance]?.remove(insert)
            }
            // add item to the new tier    (2), (3), (4)
            getTier(newDistance).add(insert)
        }
    }

    /**
     * Dequeue a states with the smallest distance.
     *
     * Worst case linear, but very fast (a predictable branch per iteration)
     */
    fun remove(): Set<Pair<State, State?>> {
        for (distance in tiers.indices) {
            val tier = tiers[distance]
            if (tier != null) {
                tiers[distance] = null
                return tier
            }
        }
        throw IllegalStateException("StateQueue is empty.")
    }

    /**
     * Return true if this queue is empty.
     *
     * Optimized for negative result. Worst case linear.
     */
    fun isEmpty(): Boolean = tiers.all { it == null }

    /**
     * Return true if this queue it not empty.
     *
     * Optimized for positive result. Worst case linear.
     */
    // statistically, non-empty tiers will be near the end
    fun isNotEmpty(): Boolean = tiers.asReversed().any { it != null }

    override fun iterator(): Iterator<Set<Pair<State, State?>>> = object : Iterator<Set<Pair<State, State?>>> {
        override fun hasNext(): Boolean = isNotEmpty()
        override fun next(): Set<Pair<State, State?>> = remove()
    }

}