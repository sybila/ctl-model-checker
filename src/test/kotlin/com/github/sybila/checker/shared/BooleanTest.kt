package com.github.sybila.checker.shared

import com.github.sybila.checker.Transition
import com.github.sybila.checker.shared.solver.IntSetSolver
import com.github.sybila.checker.shared.solver.intSetOf
import com.github.sybila.checker.shared.solver.toParams
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
) : TransitionSystem, Solver by IntSetSolver(fullColors) {

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Params>> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Params>> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun Formula.Atom.Float.eval(): StateMap {
        val bounds = 0 until stateCount
        return when (this) {
            p1 -> bounds.filter { it % 2 == 0 }.associateBy({it}, {
                intSetOf(3, 4, if (it % 4 == 0) 1 else 2)
            }).asStateMap()
            p2 -> bounds.filter { it % 3 == 0 }.associateBy({it}, {
                intSetOf(2, 3, if (it % 9 == 0) 1 else 4)
            }).asStateMap()
            p3 -> bounds.filter { it % 5 == 0 }.associateBy({it}, {
                intSetOf(1, 4, if (it % 25 == 0) 3 else 2)
            }).asStateMap()
            else -> emptyStateMap()
        }
    }

    override fun Formula.Atom.Transition.eval(): StateMap {
        throw UnsupportedOperationException("not implemented")
    }

}

abstract class BooleanVerificationTest {

    internal fun test(formula: Formula, stateCount: Int, parallelism: Int, expected: RegularFragment.() -> StateMap) {

        val model = RegularFragment(stateCount)

        model.run {
            Checker(model, parallelism).use { checker ->
                val result = checker.verify(formula)
                expected().assertDeepEquals(result)
            }
        }
    }

}

class AndVerificationTest : BooleanVerificationTest() {

    private fun RegularFragment.simpleExpected(): StateMap {
        return (0 until stateCount).filter { it % 2 == 0 && it % 3 == 0 }
                .associateBy({it}, {
                    var c = setOf(3)
                    if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
                    if (it % 4 != 0) c += setOf(2)
                    if (it % 9 != 0) c += setOf(4)
                    c.toParams()
                }).asStateMap()
    }

    private fun RegularFragment.nestedExpected(): StateMap {
        return (0 until stateCount).filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }
                .associateBy({it}, {
                    var c = setOf<Int>()
                    if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
                    if (it % 4 != 0 && it % 25 != 0) c += setOf(2)
                    if (it % 25 == 0) c += setOf(3)
                    if (it % 9 != 0) c += setOf(4)
                    c.toParams()
                }).asStateMap()
    }

    @Test
    fun simpleTest() = test(p1 and p2, 2000, 1) { simpleExpected() }

    @Test
    fun nestedTest() = test(p1 and p2 and p3, 2000, 1) { nestedExpected() }

    @Test
    fun simpleConcurrentTest() = test(p1 and p2, 4000, 2) { simpleExpected() }

    @Test
    fun nestedConcurrentTest() = test(p1 and p2 and p3, 8000, 4) { nestedExpected() }

}


class OrVerificationTest : BooleanVerificationTest() {

    private fun RegularFragment.simpleExpected()
        = (0 until stateCount).filter { it % 2 == 0 || it % 3 == 0 }.associateBy({it}, {
        var c = setOf(3)
        if (it % 3 == 0 || it % 4 != 0) c += 2
        if (it % 9 == 0 || it % 4 == 0) c += 1
        if (it % 2 == 0 || it % 9 != 0) c += 4
        c.toParams()
    }).asStateMap()

    private fun RegularFragment.nestedExpected()
        = (0 until stateCount).filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.associateBy({it}, {
        var c = setOf<Int>()
        if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += 1
        if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += 2
        if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += 3
        if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += 4
        c.toParams()
    }).asStateMap()

    @Test
    fun simpleTest() = test(p1 or p2, 2000, 1) { simpleExpected() }

    @Test
    fun nestedTest() = test(p1 or p2 or p3, 2000, 1) { nestedExpected() }

    @Test
    fun simpleConcurrentTest() = test(p1 or p2, 4000, 2) { simpleExpected() }

    @Test
    fun nestedConcurrentTest() = test(p1 or p2 or p3, 8000, 4) { nestedExpected() }

}

class NegationTest : BooleanVerificationTest() {

    private fun RegularFragment.simpleExpected()
        = (0 until stateCount).associateBy({it}, {
        var c = setOf<Int>()
        if (it % 2 != 0 || it % 4 != 0) c += 1
        if (it % 2 != 0 || it % 4 == 0) c += 2
        if (it % 2 != 0) c += setOf(3, 4)
        c.toParams()
    }).asStateMap()

    @Test
    fun simpleTest() = test(not(p1), 2000, 1) { simpleExpected() }

    @Test
    fun nestedTest() = test(not(not(p1)), 2000, 1) { p1.eval() }


    @Test
    fun simpleConcurrentTest() = test(not(p1), 4000, 2) { simpleExpected() }


    @Test
    fun nestedConcurrentTest() = test(not(not(p1)), 8000, 4) { p1.eval() }

}

class MixedBooleanTest : BooleanVerificationTest() {

    @Test //obfuscated (p1 || p2) && !p3
    fun complexTest() = test((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2), 2000, 1) {
        (0 until stateCount).filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
            var c = setOf<Int>()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
            if (it % 5 != 0 || it % 25 != 0) c += 3
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
            c.toParams()
        }).asStateMap()
    }

    @Test
    fun complexConcurrentTest() = test((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2), 8000, 4) {
        (0 until stateCount).filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
            var c = setOf<Int>()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
            if (it % 5 != 0 || it % 25 != 0) c += 3
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
            c.toParams()
        }).asStateMap()
    }

}