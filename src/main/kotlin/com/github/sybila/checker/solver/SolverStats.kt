package com.github.sybila.checker.solver

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicLong

object SolverStats {

    private val printInterval = 2000

    private var output: PrintStream? = System.out
    private var startTime = System.currentTimeMillis()
    private val totalSolverCalls = AtomicLong()

    private val solverCalls = AtomicLong()
    private val lastPrint = AtomicLong(System.currentTimeMillis())

    fun reset(output: PrintStream?) {
        this.output = output
        startTime = System.currentTimeMillis()
        lastPrint.set(System.currentTimeMillis())
        totalSolverCalls.set(0)
        solverCalls.set(0)
        output?.println("Started solver throughput measuring. Metric: No. of solver calls per second.")
    }

    fun solverCall() {
        val time = System.currentTimeMillis()
        val last = lastPrint.get()
        val calls = solverCalls.incrementAndGet()
        //CAS ensures only one thread will actually print something.
        //We might lose one or two solver calls, but that really doesn't matter.
        if (time > last + printInterval && lastPrint.compareAndSet(last, time)) {
            solverCalls.set(0)
            output?.println("Throughput: ${calls/(printInterval/1000)}/s")
        }
        //update global stats
        totalSolverCalls.incrementAndGet()
    }

    fun printGlobal() {
        val total = totalSolverCalls.get()
        val duration = System.currentTimeMillis() - startTime
        output?.println("Total elapse time: $duration ms")
        output?.println("Average throughput: ${total/(duration/1000)}/s")
    }

}