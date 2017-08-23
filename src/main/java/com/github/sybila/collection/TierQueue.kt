package com.github.sybila.collection

import java.util.*

/**
 * A search queue which maintains distance of states (with respect to the order in which they are added
 * to the queue).
 *
 * It is NOT thread safe!
 */
class TierQueue<S: Any> {

    private val distance = HashMap<S, Int>()
    private val tiers: MutableList<MutableSet<S>?> = ArrayList()

    private fun getTier(distance: Int): MutableSet<S> {
        while (tiers.size <= distance) tiers.add(null)
        return tiers[distance] ?: run {
            val newTier = HashSet<S>()
            tiers[distance] = newTier
            newTier
        }
    }

    /**
     * Enqueue given [state], updating it's distance based on [from]. If [from] is null,
     * [state] distance is updated to 0.
     */
    fun add(state: S, from: S?) {
        val oldDistance = distance[state] ?: Int.MAX_VALUE
        val newDistance = if (from == null) 0 else {
            Math.min(oldDistance, (distance[from] ?: 0) + 1)
        }
        distance[state] = newDistance

        if (oldDistance != newDistance) {
            tiers.getOrNull(oldDistance)?.remove(state)
        }
        getTier(newDistance).add(state)
    }

    /**
     * Dequeue a states with the smallest distance.
     *
     * Worst case linear, but very fast (a predictable branch per iteration)
     */
    fun remove(): Set<S> {
        for (distance in tiers.indices) {
            val tier = tiers[distance]
            if (tier != null) {
                tiers[distance] = null
                return tier
            }
        }
        throw IllegalStateException("StateQueue is empty.")
    }

    fun isEmpty(): Boolean = tiers.all { it == null }
    fun isNotEmpty(): Boolean = tiers.any { it != null }

}