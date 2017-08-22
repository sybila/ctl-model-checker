package com.github.sybila.collection

/**
 * Extension of [StateMap] which enables modification either using unsafe [lazySet] or atomic [compareAndSet].
 *
 * The state map is allowed to refuse modification when given state cannot be stored in
 * this instance. In such case, [IndexOutOfBoundsException] is thrown.
 */
interface MutableStateMap<S: Any, P: Any> : StateMap<S, P> {

    /**
     * Standard atomic compare-and-set operation. The value of [state] is updated to [new]
     * only if it the current value matches the [expected] value (using reference equality `===`).
     *
     * @return True if value was updated, false otherwise.
     * @throws [IndexOutOfBoundsException] when [state] cannot be stored in this state map.
     */
    fun compareAndSet(state: S, expected: P?, new: P?): Boolean

    /**
     * Standard non-atomic set operation. The value of [state] is updated to [new],
     * but no hard guarantees are given about ordering of such operations in case of races.
     *
     * @throws [IndexOutOfBoundsException] when [state] cannot be stored in this state map.
     */
    fun lazySet(state: S, new: P?)

}