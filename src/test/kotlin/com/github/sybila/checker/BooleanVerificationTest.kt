package com.github.sybila.checker

import com.github.sybila.checker.new.*
import com.github.sybila.huctl.*
import org.junit.Test
import kotlin.test.assertEquals

//TODO: Tests for implication and equivalence

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
private class RegularFragment(
        private val bounds: IntRange
) : Fragment<Set<Int>> {

    override val id: Int = 0
    override fun Int.owner(): Int = 0

    override fun step(from: Int, future: Boolean): Iterator<Transition<Set<Int>>> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun eval(atom: Formula.Atom): StateMap<Set<Int>> {
        return when (atom) {
            Formula.Atom.True -> bounds.associateBy({it}, { fullColors }).asStateMap(setOf())
            p1 -> bounds.filter { it % 2 == 0 }.associateBy({it}, {
                setOf(3, 4, if (it % 4 == 0) 1 else 2)
            }).asStateMap(setOf())
            p2 -> bounds.filter { it % 3 == 0 }.associateBy({it}, {
                setOf(2, 3, if (it % 9 == 0) 1 else 4)
            }).asStateMap(setOf())
            p3 -> bounds.filter { it % 5 == 0 }.associateBy({it}, {
                setOf(1, 4, if (it % 25 == 0) 3 else 2)
            }).asStateMap(setOf())
            else -> mapOf<Int, Set<Int>>().asStateMap(setOf())
        }
    }

}

class AndVerificationTest {

    @Test
    fun simpleTest() {
        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(p1 and p2)[0]

        val expected = (bounds).filter { it % 2 == 0 && it % 3 == 0 }.associateBy({it}, {
            var c = setOf(3)
            if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
            if (it % 4 != 0) c += setOf(2)
            if (it % 9 != 0) c += setOf(4)
            c
        }).asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }


    @Test
    fun nestedTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(p1 and p2 and p3)[0]

        val expected = (bounds).filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }.associateBy({it}, {
            var c = setOf<Int>()
            if (it % 4 == 0 && it % 9 == 0) c += setOf(1)
            if (it % 4 != 0 && it % 25 != 0) c += setOf(2)
            if (it % 25 == 0) c += setOf(3)
            if (it % 9 != 0) c += setOf(4)
            c
        }).asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }

    @Test
    fun nestedConcurrentTest() {

        val partitions = listOf(0..1667, 1668..4232, 4232..8000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.indices.map { EnumerativeSolver(fullColors) }
        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(p1 and p2 and p3)

        val expected = partitions.map {
            it.filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }.associateBy({ it }, {
                var c = setOf<Int>()
                if (it % 4 == 0 && it % 9 == 0) c += 1
                if (it % 4 != 0 && it % 25 != 0) c += 2
                if (it % 25 == 0) c += 3
                if (it % 9 != 0) c += 4
                c
            }).asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }

    @Test
    fun simpleConcurrentTest() {

        val partitions = listOf(0..1867, 1868..4000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.map { EnumerativeSolver(fullColors) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(p1 and p2)

        val expected = partitions.map {
            it.filter { it % 2 == 0 && it % 3 == 0 }.associateBy({ it }, {
                var c = setOf(3)
                if (it % 4 == 0 && it % 9 == 0) c += 1
                if (it % 4 != 0) c += 2
                if (it % 9 != 0) c += 4
                c
            }).asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }

}


class OrVerificationTest {

    @Test
    fun simpleTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(p1 or p2)[0]

        val expected = (bounds).filter { it % 2 == 0 || it % 3 == 0 }.associateBy({it}, {
            var c = setOf(3)
            if (it % 3 == 0 || it % 4 != 0) c += 2
            if (it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 2 == 0 || it % 9 != 0) c += 4
            c
        }).asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }

    @Test
    fun nestedTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(p1 or p2 or p3)[0]

        val expected = (bounds).filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.associateBy({it}, {
            var c = setOf<Int>()
            if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += 1
            if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += 2
            if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += 3
            if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += 4
            c
        }).asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }

    @Test
    fun simpleConcurrentTest() {

        val partitions = listOf(0..1867, 1868..4000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.map { EnumerativeSolver(fullColors) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(p1 or p2)

        val expected = partitions.map {
            it.filter { it % 2 == 0 || it % 3 == 0 }.associateBy({ it }, {
                var c = setOf(3)
                if (it % 3 == 0 || it % 4 != 0) c += 2
                if (it % 9 == 0 || it % 4 == 0) c += 1
                if (it % 2 == 0 || it % 9 != 0) c += 4
                c
            }).asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }


    @Test
    fun nestedConcurrentTest() {

        val partitions = listOf(0..1667, 1668..4232, 4232..8000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.indices.map { EnumerativeSolver(fullColors) }
        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(p1 or p2 or p3)

        val expected = partitions.map {
            it.filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.associateBy({ it }, {
                var c = setOf<Int>()
                if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += 1
                if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += 2
                if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += 3
                if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += 4
                c
            }).asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }

}

class NegationTest {

    @Test
    fun simpleTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(not(p1))[0]

        val expected = (bounds).associateBy({it}, {
            var c = setOf<Int>()
            if (it % 2 != 0 || it % 4 != 0) c += 1
            if (it % 2 != 0 || it % 4 == 0) c += 2
            if (it % 2 != 0) c += setOf(3, 4)
            c
        }).asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }

    @Test
    fun nestedTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)

        val checker = Checker(model, solver)

        val result = checker.verify(not(not(p1)))[0]

        val expected = model.eval(p1)

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)


    }


    @Test
    fun simpleConcurrentTest() {

        val partitions = listOf(0..1867, 1868..4000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.map { EnumerativeSolver(fullColors) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(not(p1))

        val expected = partitions.map {
            it.associateBy({ it }, {
                var c = setOf<Int>()
                if (it % 2 != 0 || it % 4 != 0) c += 1
                if (it % 2 != 0 || it % 4 == 0) c += 2
                if (it % 2 != 0) c += setOf(3, 4)
                c
            }).asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }


    @Test
    fun nestedConcurrentTest() {

        val partitions = listOf(0..1667, 1668..4232, 4232..8000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.indices.map { EnumerativeSolver(fullColors) }
        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify(not(not(p1)))

        val expected = models.map { it.eval(p1) }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }

    }
}

class MixedBooleanTest() {

    @Test
    fun complexTest() {

        val bounds = 0..2000

        val model = RegularFragment(bounds)
        val solver = EnumerativeSolver(fullColors)
        val checker = Checker(model, solver)

        //obfuscated (p1 || p2) && !p3
        val result = checker.verify((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2))[0]

        val expected = (bounds).filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
            var c = setOf<Int>()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
            if (it % 5 != 0 || it % 25 != 0) c += 3
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
            c
        }).filter { it.value.isNotEmpty() }.asStateMap(setOf())

        assertEquals(expected.toSet(), result.toSet())
        expected.assertDeepEquals(result, solver)

    }

    @Test
    fun complexConcurrentTest() {

        val partitions = listOf(0..1896, 1897..3975, 3976..8000)

        val models = partitions.map(::RegularFragment)
        val solvers = partitions.map { EnumerativeSolver(fullColors) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        val result = checker.verify((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2))

        val expected = partitions.map {
            it.filter { (it % 2 == 0 || it % 3 == 0) }.associateBy({it}, {
                var c = setOf<Int>()
                if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += 1
                if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += 2
                if (it % 5 != 0 || it % 25 != 0) c += 3
                if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += 4
                c
            }).filter { it.value.isNotEmpty() }.asStateMap(setOf())
        }

        for (p in partitions.indices) {
            assertEquals(expected[p].toSet(), result[p].toSet())
            expected[p].assertDeepEquals(result[p], solvers[p])
        }
    }

}