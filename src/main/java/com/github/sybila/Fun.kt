package com.github.sybila

import com.github.sybila.funn.ModelChecker
import com.github.sybila.funn.ode.ODETransitionSystem
import com.github.sybila.huctl.parser.readHUCTLp
import com.github.sybila.ode.model.Parser
import java.io.File
import java.io.PrintStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.coroutines.experimental.buildSequence
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

-XX:+UnlockDiagnosticVMOptions -XX:+TraceClassLoading -XX:+LogCompilation -XX:+PrintAssembly
 */
fun main(args: Array<String>) {

    println("Test run: ${measureTimeMillis { action() }}")

    val results = ArrayList<Long>()
    repeat(5) {

        val elapsed = measureTimeMillis {
            action()

        }

        println("Elapsed: $elapsed")
        results.add(elapsed)
    }

    println("Avr.: ${results.sum() / 5}")

}

fun action() {
    val mFile = File("model.bio")
    val pFile = File("prop.ctl")
    //val oFile = File("/Users/daemontus/Downloads/fun_result.json")

    val model = Parser().parse(mFile)
    val prop = readHUCTLp(pFile, onlyFlagged = true)

    println(prop)
    println(model)

    val tSystem = ODETransitionSystem(model)
    val mc = ModelChecker(model = tSystem, maps = tSystem, sets = tSystem.sets, solver = tSystem.solver, fork = 4, meanChunkTime = 25)

    val s = prop["stay_high"]!!

    val result = mc.check(prop.map { it.key to it.value })
    //val result = mc.check(listOf("stay_high" to s))

    /*PrintStream(oFile.apply { this.createNewFile() }.outputStream()).use { outStream ->
        outStream.println(printJsonRectResults(model, result.toMap()))
    }*/
}

class Foo {
    private val set = HashSet<Unit>()
    private val map = HashMap<Unit, Unit>()

    init {
        if (Unit in set) println("a")
        if (Unit in map) println("b")   // unnecessary null check when using `in` operator
    }
}