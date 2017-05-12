package com.github.sybila.checker

import com.github.sybila.checker.channel.connectWithSharedMemory
import com.github.sybila.checker.partition.IntervalPartition
import com.github.sybila.checker.partition.asIntervalPartitions
import com.github.sybila.huctl.*
import org.junit.Test

private val p1 = "x".asVariable() eq 3.0.asConstant()
private val p2 = "y".asVariable() gt 3.4.asConstant()
private val p3 = "z".asVariable() lt 1.4.asConstant()

private val fullColors = (1..4).toSet()

/**
 * This is a simple one dimensional model with fixed number of states, no transitions and
 * propositions distributed using modular arithmetic, so that their
 * validity can be easily predicted (although it might require some nontrivial
 * control flow)
 */
internal class RegularFragment(
        override val stateCount: Int
) : Model<Set<Int>>, Solver<Set<Int>> by IntSetSolver(fullColors) {

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Set<Int>>> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Set<Int>>> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun Formula.Atom.Float.eval(): StateMap<Set<Int>> {
        val bounds = 0 until stateCount
        return when (this) {
            p1 -> bounds.filter { it % 2 == 0 }.associateBy({it}, {
                setOf(3, 4, if (it % 4 == 0) 1 else 2)
            }).asStateMap()
            p2 -> bounds.filter { it % 3 == 0 }.associateBy({it}, {
                setOf(2, 3, if (it % 9 == 0) 1 else 4)
            }).asStateMap()
            p3 -> bounds.filter { it % 5 == 0 }.associateBy({it}, {
                setOf(1, 4, if (it % 25 == 0) 3 else 2)
            }).asStateMap()
            else -> emptyStateMap()
        }
    }

    override fun Formula.Atom.Transition.eval(): StateMap<Set<Int>> {
        throw UnsupportedOperationException("not implemented")
    }

}

abstract class BooleanVerificationTest {

    internal fun sequentialTest(formula: Formula, expected: RegularFragment.() -> StateMap<Set<Int>>) {
        val stateCount = 2000

        val model = RegularFragment(stateCount)

        model.run {
            SequentialChecker(model).use { checker ->
                val result = checker.verify(formula)
                expected().assertDeepEquals(result)
            }
        }
    }

    internal fun concurrentTest(formula: Formula, intervals: List<IntRange>, expected: IntervalPartition<Set<Int>>.() -> StateMap<Set<Int>>) {
        val stateCount = intervals.last().last + 1
        val partitionCount = intervals.size

        val models = (1..stateCount).map { RegularFragment(stateCount) }
        val partitions = models.zip(intervals).asIntervalPartitions()

        Checker(partitions.connectWithSharedMemory()).use { checker ->
            checker.verify(formula).zip(partitions).forEach {
                val (result, partition) = it
                partition.run {
                    expected().assertDeepEquals(result)
                }
            }
        }
    }

    internal fun smallConcurrentTest(formula: Formula, expected: IntervalPartition<Set<Int>>.() -> StateMap<Set<Int>>)
        = concurrentTest(formula, listOf(0..1867, 1868..3999), expected)

    internal fun mediumConcurrentTest(formula: Formula, expected: IntervalPartition<Set<Int>>.() -> StateMap<Set<Int>>)
            = concurrentTest(formula, listOf(0..1667, 1668..4231, 4232..7999), expected)


}

class AndVerificationTest : BooleanVerificationTest() {

    @Test
    fun simpleTest() = sequentialTest(p1 and p2) {
        (0 until stateCount).filter { it % 2 == 0 && it % 3 == 0 }
        .associateBy({it}, {
            var c = setOf(3)
            if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
            if (it % 4 != 0) c += setOf(2)
            if (it % 9 != 0) c += setOf(4)
            c
        }).asStateMap()
    }

    @Test
    fun nestedTest() = sequentialTest(p1 and p2 and p3) {
        (0 until stateCount).filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }
        .associateBy({it}, {
            var c = setOf<Int>()
            if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
            if (it % 4 != 0 && it % 25 != 0) c += setOf(2)
            if (it % 25 == 0) c += setOf(3)
            if (it % 9 != 0) c += setOf(4)
            c
        }).asStateMap()
    }

    @Test
    fun simpleConcurrentTest() = smallConcurrentTest(p1 and p2) {
        myInterval().filter { it % 2 == 0 && it % 3 == 0 }
        .associateBy({ it }, {
            var c = setOf(3)
            if (it % 4 == 0 && it % 9 == 0) c += 1
            if (it % 4 != 0) c += 2
            if (it % 9 != 0) c += 4
            c
        }).asStateMap()
    }

    @Test
    fun nestedConcurrentTest() = mediumConcurrentTest(p1 and p2 and p3) {
        myInterval().filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }
        .associateBy({ it }, {
            var c = setOf<Int>()
            if (it % 4 == 0 && it % 9 == 0) c += 1
            if (it % 4 != 0 && it % 25 != 0) c += 2
            if (it % 25 == 0) c += 3
            if (it % 9 != 0) c += 4
            c
        }).asStateMap()
    }

}


class OrVerificationTest : BooleanVerificationTest() {

    @Test
    fun simpleTest() = sequentialTest(p1 or p2) {
        (0 until stateCount).filter { it % 2 == 0 || it % 3 == 0 }.associateBy({it}, {
            var c = setOf(3)
            if (it % 3 == 0 || it % 4 != 0) c += 2
            if (it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 2 == 0 || it % 9 != 0) c += 4
            c
        }).asStateMap()
    }

    @Test
    fun nestedTest() = sequentialTest(p1 or p2 or p3) {
        (0 until stateCount).filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.associateBy({it}, {
            var c = setOf<Int>()
            if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += 2
            if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += 3
            if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += 4
            c
        }).asStateMap()
    }

    @Test
    fun simpleConcurrentTest() = smallConcurrentTest(p1 or p2) {
        myInterval().filter { it % 2 == 0 || it % 3 == 0 }.associateBy({ it }, {
            var c = setOf(3)
            if (it % 3 == 0 || it % 4 != 0) c += 2
            if (it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 2 == 0 || it % 9 != 0) c += 4
            c
        }).asStateMap()
    }


    @Test
    fun nestedConcurrentTest() = mediumConcurrentTest(p1 or p2 or p3) {
        myInterval().filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.associateBy({ it }, {
            var c = setOf<Int>()
            if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += 2
            if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += 3
            if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += 4
            c
        }).asStateMap()
    }

}

class NegationTest : BooleanVerificationTest() {

    @Test
    fun simpleTest() = sequentialTest(not(p1)) {
        (0 until stateCount).associateBy({it}, {
            var c = setOf<Int>()
            if (it % 2 != 0 || it % 4 != 0) c += 1
            if (it % 2 != 0 || it % 4 == 0) c += 2
            if (it % 2 != 0) c += setOf(3, 4)
            c
        }).asStateMap()
    }

    @Test
    fun nestedTest() = sequentialTest(not(not(p1))) { p1.eval() }


    @Test
    fun simpleConcurrentTest() = smallConcurrentTest(not(p1)) {
        myInterval().associateBy({ it }, {
            var c = setOf<Int>()
            if (it % 2 != 0 || it % 4 != 0) c += 1
            if (it % 2 != 0 || it % 4 == 0) c += 2
            if (it % 2 != 0) c += setOf(3, 4)
            c
        }).asStateMap()
    }


    @Test
    fun nestedConcurrentTest() = mediumConcurrentTest(not(not(p1))) { p1.eval().restrictToPartition() }
}

class MixedBooleanTest() : BooleanVerificationTest() {

    @Test //obfuscated (p1 || p2) && !p3
    fun complexTest() = sequentialTest((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2)) {
        (0 until stateCount).filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
            var c = setOf<Int>()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
            if (it % 5 != 0 || it % 25 != 0) c += 3
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
            c
        }).asStateMap()
    }

    @Test
    fun complexConcurrentTest() = mediumConcurrentTest((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2)) {
        myInterval().filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
            var c = setOf<Int>()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
            if (it % 5 != 0 || it % 25 != 0) c += 3
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
            c
        }).asStateMap()
    }

}