package com.github.sybila.collection

/**
 * A mutable variant of [StateSet] which supports both lazy an atomic operations.
 *
 * All get operations are assumed to be atomic!
 *
 * The set is allowed to refuse modification when given state cannot be stored in
 * this instance. In such case, [IndexOutOfBoundsException] is thrown.
 */
interface MutableStateSet<S: Any> : StateSet<S> {


    /**
     * Atomically add given [state] to the set.
     *
     * @return True is the state was not present and it was successfully added.
     * @throws [IndexOutOfBoundsException] if the state cannot be saved to this set.
     */
    fun atomicAdd(state: S): Boolean

    /**
     * Atomically remove given [state] from the set.
     *
     * @return True if the state was present and it was successfully removed.
     */
    fun atomicRemove(state: S): Boolean

    /**
     * Standard non-atomic add operation. The contract is that eventually, [state]
     * is added to the set according to the standard weak guarantees of JVM memory model.
     *
     * @throws [IndexOutOfBoundsException] if the state cannot be saved to this set.
     */
    fun lazyAdd(state: S)

    /**
     * Standard non-atomic remove operation. No hard timing guarantees, just eventual consistency.
     */
    fun lazyRemove(state: S)

}