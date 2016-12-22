package com.github.sybila.checker.explicit

import com.github.sybila.checker.new.*
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.positiveIn
import org.junit.Test
import kotlin.test.assertEquals

class ExplicitEXTest {

    val atom = "x".positiveIn() as Formula.Atom //not so useless

    @Test
    fun emptyInner() {
        val model = ExplicitFragment(
                mapOf(
                        0 to listOf(Transition(1, "x".increaseProp(), true)),
                        1 to listOf(Transition(0, "x".decreaseProp(), true))
                ),
                mapOf(atom to mapOf<Int, Boolean>()), BOOL_SOLVER
        )
        assertEquals(mapOf<Int, Boolean>().asStateMap(false), model.eval())
    }

}