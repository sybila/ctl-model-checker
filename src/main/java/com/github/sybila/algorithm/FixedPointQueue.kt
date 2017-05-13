package com.github.sybila.algorithm

interface FixedPointQueue<I> : Iterable<Iterable<I>> {

    fun add(item: I, from: I?)

    fun remove(): Iterable<I>

}