package com.github.sybila.model

interface IncreasingStateMap<State : Any, Param : Any> : StateMap<State, Param> {

    fun increaseKey(key: State, value: Param): Boolean

}