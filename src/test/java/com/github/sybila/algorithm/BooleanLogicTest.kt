package com.github.sybila.algorithm

import com.github.sybila.collection.GenericStateMap
import com.github.sybila.collection.GenericCollectionContext
import com.github.sybila.collection.CollectionContext
import com.github.sybila.coroutines.lazyAsync
import com.github.sybila.solver.SetSolver
import com.github.sybila.solver.Solver
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.test.assertEquals

class BooleanLogicTest {

    private fun makeAlgorithm(): BooleanLogic<String, Set<Int>> {
        val solver = SetSolver(setOf(1,2,3,4))
        val maps = GenericCollectionContext(listOf("a", "b", "c", "d").map { it to solver.TT }.toMap())
        return object : BooleanLogic<String, Set<Int>>, CollectionContext<String, Set<Int>> by maps {
            override val fork: Int = 4
            override val solver: Solver<Set<Int>> = solver
            override val executor: CoroutineContext = CommonPool
            override val meanChunkTime: Long = 25
        }
    }

    private val A = GenericStateMap(
            "a" to setOf(1,2,3), "b" to setOf(2,3,4), "c" to setOf(1,2,3)
    )
    private val B = GenericStateMap(
            "a" to setOf(2,3,4), "b" to setOf(1,2,3), "d" to setOf(2,3,4)
    )

    @Test
    fun andTest() {
        makeAlgorithm().run {
            runBlocking {
                val C = makeAnd(lazyAsync(executor) { A }, lazyAsync(executor) { B }).await()
                assertEquals(mapOf(
                        "a" to setOf(2, 3), "b" to setOf(2,3)
                ), C.entries.toMap())
            }
        }
    }

    @Test
    fun orTest() {
        makeAlgorithm().run {
            runBlocking {
                val C = makeOr(lazyAsync(executor) { A }, lazyAsync(executor) { B }).await()
                assertEquals(mapOf(
                        "a" to setOf(1,2,3,4), "b" to setOf(1,2,3,4), "c" to setOf(1,2,3), "d" to setOf(2,3,4)
                ), C.entries.toMap())
            }
        }
    }

    @Test
    fun makeComplement() {
        makeAlgorithm().run {
            runBlocking {
                val C = makeComplement(lazyAsync(executor) { A }, lazyAsync(executor) { B }).await()
                assertEquals(mapOf(
                        "a" to setOf(4), "b" to setOf(1), "d" to setOf(2,3,4)
                ), C.entries.toMap())
            }
        }
    }

}