package com.github.sybila.algorithm

import com.github.sybila.model.toStateMap
import com.github.sybila.solver.SetSolver
import org.junit.Test
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

// Warning! All these tests work because syntactic and semantic equality of sets
// is the same. Do not try this with symbolic representations.

sealed class BooleanLogicTest(override final val stateCount: Int) : BooleanLogic<Set<Int>> {

    override final val solver = SetSolver((1..4).toSet())

    protected val bounds = 0 until stateCount

    protected val p1 = bounds.filter { it % 2 == 0 }.associateBy({it}, {
        setOf(3, 4, if (it % 4 == 0) 1 else 2)
    }).toStateMap(solver, stateCount).asMono()

    protected val p2 = bounds.filter { it % 3 == 0 }.associateBy({it}, {
        setOf(2, 3, if (it % 9 == 0) 1 else 4)
    }).toStateMap(solver, stateCount).asMono()

    protected val p3 = bounds.filter { it % 5 == 0 }.associateBy({it}, {
        setOf(1, 4, if (it % 25 == 0) 3 else 2)
    }).toStateMap(solver, stateCount).asMono()

    sealed class Disjunction : BooleanLogicTest(2000) {

        @Test
        fun simpleTest() {
            val expected = bounds.associateBy({it}, {
                var c = setOf<Int>()
                if (it % 2 == 0) c += setOf(3, 4, if (it % 4 == 0) 1 else 2)    // add p1
                if (it % 3 == 0) c += setOf(2, 3, if (it % 9 == 0) 1 else 4)    // add p2
                c
            }).toStateMap(solver, stateCount)

            val result = p1.disjunction(p2).block()
            assertMapEquals(expected, result, bounds)
        }

        @Test
        fun nestedTest() {
            val expected = bounds.associateBy({it}, {
                var c = setOf<Int>()
                if (it % 2 == 0) c += setOf(3, 4, if (it % 4 == 0) 1 else 2)    // add p1
                if (it % 3 == 0) c += setOf(2, 3, if (it % 9 == 0) 1 else 4)    // add p2
                if (it % 5 == 0) c += setOf(1, 4, if (it % 25 == 0) 3 else 2)    // add p3
                c
            }).toStateMap(solver, stateCount)

            val result = p1.disjunction(p2.disjunction(p3)).block()
            assertMapEquals(expected, result, bounds)
        }

        class Parallel : Disjunction() {
            override val scheduler: Scheduler = Schedulers.parallel()
        }

        class Sequential : Disjunction() {
            override val scheduler: Scheduler = Schedulers.single()
        }

    }

    sealed class Complement : BooleanLogicTest(2000) {

        @Test
        fun simpleTest() {
            val expected = bounds.associateBy({it}, {
                var c = solver.tt
                if (it % 2 == 0) c -= setOf(3, 4, if (it % 4 == 0) 1 else 2)    // remove p1
                c
            }).toStateMap(solver, stateCount)

            val result = p1.complement().block()
            assertMapEquals(expected, result, bounds)
        }

        @Test
        fun nestedTest() {
            val expected = p1.block()
            val result = p1.complement().complement().block()
            assertMapEquals(expected, result, bounds)
        }

        @Test
        fun withUniverse() {
            val expected = bounds.associateBy({it}, {
                var c = setOf<Int>()
                if (it % 3 == 0) c += setOf(2, 3, if (it % 9 == 0) 1 else 4)    // add p2
                if (it % 2 == 0) c -= setOf(3, 4, if (it % 4 == 0) 1 else 2)    // remove p1
                c
            }).toStateMap(solver, stateCount)

            val result = p1.complement(p2).block()
            assertMapEquals(expected, result, bounds)
        }

        class Parallel : Complement() {
            override val scheduler: Scheduler = Schedulers.parallel()
        }

        class Sequential : Complement() {
            override val scheduler: Scheduler = Schedulers.single()
        }

    }

    sealed class Conjunction : BooleanLogicTest(10) {

        @Test
        fun simpleTest() {
            val expected = bounds.filter { it % 2 == 0 && it % 3 == 0 }
                    .associateBy({it}, {
                        var c = setOf(3)
                        if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
                        if (it % 4 != 0) c += setOf(2)
                        if (it % 9 != 0) c += setOf(4)
                        c
                    }).toStateMap(solver, stateCount)

            val result = p1.conjunction(p2).block()
            assertMapEquals(expected, result, bounds)
        }

        @Test
        fun nestedTest() {
            val expected = bounds.filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }
                    .associateBy({it}, {
                        var c = setOf<Int>()
                        if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
                        if (it % 4 != 0 && it % 25 != 0) c += setOf(2)
                        if (it % 25 == 0) c += setOf(3)
                        if (it % 9 != 0) c += setOf(4)
                        c
                    }).toStateMap(solver, stateCount)

            val result = p1.conjunction(p2.conjunction(p3)).block()
            assertMapEquals(expected, result, bounds)
        }

        class Parallel : Conjunction() {
            override val scheduler: Scheduler = Schedulers.parallel()
        }

        class Sequential : Conjunction() {
            override val scheduler: Scheduler = Schedulers.single()
        }

    }

}