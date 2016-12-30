package com.github.sybila.checker

import com.github.sybila.huctl.EF
import org.openjdk.jmh.annotations.Benchmark

open class Benchmarks {

    @Benchmark
    fun benchEF(model: SimpleReachModel): Int {
        return SequentialChecker(model).use {
            it.verify(EF(SimpleReachModel.Prop.UPPER_CORNER())).sizeHint
        }
    }

}