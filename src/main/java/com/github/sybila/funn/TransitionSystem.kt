package com.github.sybila.funn

import com.github.sybila.huctl.Formula

interface TransitionSystem<S, P> {

    val fullMap: StateMap<S, P>
    val emptyMap: StateMap<S, P>

    fun makeProposition(proposition: Formula): StateMap<S, P>

    fun mutate(stateMap: StateMap<S, P>): MutableStateMap<S, P>

}