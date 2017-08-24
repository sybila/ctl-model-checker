package com.github.sybila.algorithm

import com.github.sybila.solver.Solver
import kotlin.coroutines.experimental.CoroutineContext

interface Algorithm<S: Any, P: Any> {

    val fork: Int
    val solver: Solver<P>
    val executor: CoroutineContext
    val meanChunkTime: Long

}