package com.github.sybila.funn

import com.github.sybila.collection.MutableStateMap
import com.github.sybila.collection.StateMap
import com.github.sybila.huctl.Formula

interface TransitionSystem<S:Any, P:Any> {

    val fullMap: StateMap<S, P>
    val emptyMap: StateMap<S, P>

    fun makeProposition(proposition: Formula): StateMap<S, P>

    fun mutate(stateMap: StateMap<S, P>): MutableStateMap<S, P>

    fun <T : Any> genericMap(zero: T): MutableStateMap<S, T>

    fun nextStep(from: S, timeFlow: Boolean): Iterable<Pair<S, P>>

}