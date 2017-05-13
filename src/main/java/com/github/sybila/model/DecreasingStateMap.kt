package com.github.sybila.model

interface DecreasingStateMap<State: Any, Param : Any> : StateMap<State, Param> {

    fun decreaseKey(key: Int, value: Param): Boolean

}