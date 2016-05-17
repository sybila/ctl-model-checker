package com.github.sybila.checker

import com.github.sybila.checker.uctl.UAU
import com.github.sybila.checker.uctl.UBind
import com.github.sybila.checker.uctl.UModelChecker
import com.github.sybila.checker.uctl.UProposition
import com.github.sybila.ctl.True


fun main(args: Array<String>) {

    //val property = UEU(true, UProposition(True), UProposition(True), Direction(0, true), anyDirection)
    /*val property = UBind("x", UEU(
            true,
            UProposition(True),
            UName("x"),
            anyDirection, anyDirection
    ))*/
    val property = UBind("x", UProposition(ReachModel.Prop.UPPER_CORNER))
    val fragment = ReachModel(2, 10)

    println("Normalized formula: $property")

    val checker = UModelChecker(fragment, IDColors(), fragment.parameters)
    println("All: ${fragment.allNodes().entries.count()}")
    val results = checker.verify(property, mapOf())
    println("Result: ${results.entries.count()}")
    for (entry in results.entries) {
        println(entry)
    }
}