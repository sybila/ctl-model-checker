package com.github.sybila.checker

import com.github.sybila.checker.uctl.*
import com.github.sybila.ctl.True


fun main(args: Array<String>) {

    //val property = UEU(true, UProposition(True), UProposition(True), Direction(0, true), anyDirection)
    /*val property = UBind("x", UEU(
            true,
            UProposition(True),
            UName("x"),
            anyDirection, anyDirection
    ))*/
    val property = UBind("x", UEX(true, UAU(
            true, UProposition(True), UName("x"), anyDirection, anyDirection
    ), anyDirection))
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