package cz.muni.fi.checker

import java.util.*

/**
 * Although kotlin provides a map with default value, this explicit data
 * structure enables us to check for it's presence at compile time and
 * has more convenient semantics.
 */

/**
 * A read only map with default value.
 *
 * The equivalence relation on this class does not have to be implemented reliably, since
 * a lot of color sets are represented symbolically and therefore direct comparison is
 * often unfeasible.
 *
 * Therefore avoid using equals/hashCode with Nodes.
 *
 * Nodes should be immutable and therefore thread safe.
 */
interface Nodes<N: Node, C: Colors<C>> {

    /**
     * Default element returned
     */
    val emptyColors: C

    /**
     * All entries that are not empty.
     */
    val entries: Iterable<Map.Entry<N, C>>

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    fun toMutableNodes(): MutableNodes<N, C>

    fun toMutableMap(): MutableMap<N, C>

    /**
     * Return color set for given key or empty set if no value is set.
     */
    operator fun get(key: N): C

    operator fun contains(key: N): Boolean

    operator fun plus(other: Nodes<N, C>): Nodes<N, C>

    operator fun minus(other: Nodes<N, C>): Nodes<N, C>

    infix fun intersect(other: Nodes<N, C>): Nodes<N, C>

    infix fun union(other: Nodes<N, C>): Nodes<N, C> = this + other

    infix fun subtract(other: Nodes<N, C>): Nodes<N, C> = this - other

}

/**
 * Mutable variant of node set.
 *
 * Implementations should be thread safe!
 */
interface MutableNodes<N: Node, C: Colors<C>>: Nodes<N, C> {

    /**
     * When updating elements, union is created instead of replacing old value.
     * Therefore, mutable node set can only grow.
     *
     * Returns true if something has been added to the set.
     */
    fun putOrUnion(key: N, value: C): Boolean

    /**
     * Make immutable copy
     */
    fun toNodes(): Nodes<N, C>
}

open class MapNodes<N: Node, C: Colors<C>>(
        override val emptyColors: C,
        private val map: Map<N, C>
) : Nodes<N, C> {

    override val entries: Iterable<Map.Entry<N, C>> = map.entries

    override fun get(key: N): C = map.getOrElse(key) { emptyColors }

    override fun plus(other: Nodes<N, C>): Nodes<N, C> {
        val new = HashMap(map)
        for ((k, v) in other.entries) {
            new[k] = get(k) + v
        }
        return MapNodes(emptyColors, new)
    }

    override fun minus(other: Nodes<N, C>): Nodes<N, C> {
        val new = HashMap(map)
        for ((k, v) in other.entries) {
            new[k] = get(k) - v
        }
        return MapNodes(emptyColors, new.filterValues { it.isNotEmpty() })
    }

    override fun intersect(other: Nodes<N, C>): Nodes<N, C> {
        val new = HashMap<N, C>()
        for ((k, v) in other.entries) {
            new[k] = get(k) intersect v
        }
        return MapNodes(emptyColors, new.filterValues { it.isNotEmpty() })
    }

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun contains(key: N): Boolean = key in map

    override fun toMutableNodes(): MutableNodes<N, C> = MutableMapNodes(emptyColors, map)

    override fun toMutableMap(): MutableMap<N, C> = HashMap(map)

    override fun equals(other: Any?): Boolean {
        if (other is MapNodes<*, *>) {
            return this.emptyColors == other.emptyColors && this.map == other.map
        } else return false
    }

    override fun hashCode(): Int {
        return this.map.hashCode() + 31 * emptyColors.hashCode()
    }

    override fun toString(): String {
        return "NodeSet(default=$emptyColors, values=$map)"
    }

}

class MutableMapNodes<N: Node, C: Colors<C>>(
        emptyColors: C,
        map: Map<N, C>
): MapNodes<N, C>(emptyColors, map), MutableNodes<N, C> {

    private val map = HashMap(map)  //defensive copy

    override fun toNodes(): Nodes<N, C> {
        synchronized(map) {
            return MapNodes(emptyColors, HashMap(map))
        }
    }

    override fun putOrUnion(key: N, value: C): Boolean {
        if (value.isEmpty()) return false   //fail fast
        synchronized(map) {
            if (!map.containsKey(key)) {    //skip subtraction test
                map.put(key, value)
                return true
            }
            val diff = value - map[key]!!
            if (diff.isEmpty()) {   //current value is superset of new value, skip!
                return false
            } else {
                map[key] = value + map[key]!!
                return true
            }
        }
    }

}

fun <N: Node, C: Colors<C>> Map<N, C>.toNodes(value: C): Nodes<N, C>
        = MapNodes(value, this)
fun <N: Node, C: Colors<C>> MutableMap<N, C>.toMutableNodes(value: C): MutableNodes<N, C>
        = MutableMapNodes(value, this)

fun <N: Node, C: Colors<C>> nodesOf(default: C, vararg pairs: Pair<N, C>): Nodes<N, C>
        = MapNodes(default, pairs.associateBy({ it.first }, { it.second }))

