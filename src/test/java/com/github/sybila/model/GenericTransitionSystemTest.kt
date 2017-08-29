package com.github.sybila.model

import com.github.sybila.collection.EmptyStateMap
import com.github.sybila.collection.GenericStateMap
import com.github.sybila.huctl.DirFormula.*
import com.github.sybila.huctl.Formula.Text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenericTransitionSystemTest {

    @Test
    fun validExample() {
        val system = GenericTransitionSystem(Long.MAX_VALUE, sequenceOf(
            ("a" to "b") to (12L to True),
                ("a" to "c") to (-3L to False),
                ("b" to "c") to (1234L to True),
                ("c" to "c") to (0L to Loop)
        ), sequenceOf(
                Text("p1") to EmptyStateMap(),
                Text("p2") to GenericStateMap("a" to 1234L, "b" to 0L)
        ))

        system.run {
            assertEquals(setOf("a", "b", "c"), states.toSet())
            assertEquals(setOf("b", "c"), "a".successors().toSet())
            assertEquals(setOf("b", "c"), "a".predecessors(false).toSet())
            assertEquals(setOf(), "a".predecessors().toSet())
            assertEquals(setOf(), "a".successors(false).toSet())
            assertEquals(setOf("c"), "b".successors().toSet())
            assertEquals(setOf("a"), "b".predecessors().toSet())
            assertEquals(setOf("c"), "c".successors().toSet())
            assertEquals(setOf("a", "b", "c"), "c".predecessors().toSet())

            assertEquals(12L, transitionBound("a", "b"))
            assertEquals(-3L, transitionBound("a", "c"))
            assertEquals(1234L, transitionBound("b", "c"))
            assertEquals(0L, transitionBound("c", "c"))
            assertEquals(null, transitionBound("b", "a"))
            assertEquals(null, transitionBound("b", "b"))

            for (s in states) {
                assertEquals(s.successors(), s.predecessors(false))
                assertEquals(s.predecessors(), s.successors(false))
                for (t in states) {
                    assertEquals(transitionBound(s, t, true), transitionBound(t, s, false))
                    assertEquals(transitionDirection(s, t, true), transitionDirection(t, s, false))
                }
            }

            assertEquals(emptyList(), makeProposition(Text("p1")).states.toList())
            val p2 = makeProposition(Text("p2"))
            assertEquals(setOf("a", "b"), p2.states.toSet())
            assertEquals(1234L, p2["a"])
            assertEquals(0L, p2["b"])

            assertFailsWith<IllegalArgumentException> { makeProposition(Text("p3")) }

        }
    }

    @Test
    fun duplicateTransition() {
        assertFailsWith<IllegalArgumentException> {
            GenericTransitionSystem("foo|goo", sequenceOf(
                    ("a" to "b") to ("foo" to True),
                    ("a" to "b") to ("goo" to Loop)
            ), emptySequence())
        }
    }

    @Test
    fun duplicateProposition() {
        assertFailsWith<IllegalArgumentException> {
            GenericTransitionSystem("foo|goo", sequenceOf(
                    ("a" to "b") to ("foo" to True)
            ), sequenceOf(
                    Text("foo") to EmptyStateMap(),
                    Text("foo") to GenericStateMap("a" to "foo")
            ))
        }
    }

    @Test
    fun unknownStateInProposition() {
        assertFailsWith<IllegalArgumentException> {
            GenericTransitionSystem("foo|goo", sequenceOf(
                    ("a" to "b") to ("foo" to True)
            ), sequenceOf(
                    Text("foo") to GenericStateMap("a" to "foo", "bb" to "goo")
            ))
        }
    }

}