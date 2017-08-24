package com.github.sybila.collection

/**
 * Interface representing a non-parametric read-only set of states.
 *
 * Since it is read-only, similar to [StateMap], it is assumed to be thread safe.
 */
interface StateSet<S: Any> : Iterable<S> {

    /** Standard test for element presence */
    operator fun contains(state: S): Boolean

}