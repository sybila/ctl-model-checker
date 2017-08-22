package com.github.sybila

import com.github.sybila.funn.ModelChecker
import com.github.sybila.funn.ode.ODETransitionSystem
import com.github.sybila.huctl.parser.readHUCTLp
import com.github.sybila.ode.model.Parser
import java.io.File
import java.io.PrintStream
import kotlin.system.measureTimeMillis

/*
1/40s
5/43s
10/29s
20/25s
30/24s
35/25s
40/26s

1:101(25ms)/105(250ms)
4:27s(25ms)
 */
fun main(args: Array<String>) {

    val elapsed = measureTimeMillis {
        val mFile = File("model.bio")
        val pFile = File("prop.ctl")
        //val oFile = File("/Users/daemontus/Downloads/fun_result.json")

        val model = Parser().parse(mFile)
        val prop = readHUCTLp(pFile, onlyFlagged = true)

        println(prop)
        println(model)

        val tSystem = ODETransitionSystem(model)
        val mc = ModelChecker(tSystem, { tSystem.solver }, 4)

        //val s = prop["stay_high"]!!

        val result = mc.check(prop.map { it.key to it.value })//mc.check(listOf("stay_high" to s))

        /*PrintStream(oFile.apply { this.createNewFile() }.outputStream()).use { outStream ->
            outStream.println(printJsonRectResults(model, result.toMap()))
        }*/

    }

    println("Elapsed: $elapsed")

}
