package com.github.sybila.checker

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object CheckerStats {

    private val printInterval = 4000

    private var output: PrintStream? = System.out
    private val totalMapReduce = AtomicLong()
    private val totalMapReduceSize = AtomicLong()

    private val operator = AtomicReference<String>("none")
    private val mapReduce = AtomicLong()
    private val mapReduceSize = AtomicLong()
    private val lastPrint = AtomicLong(System.currentTimeMillis())

    fun reset(output: PrintStream?) {
        this.output = output
        lastPrint.set(System.currentTimeMillis())
        mapReduce.set(0)
        mapReduceSize.set(0)
    }

    fun setOperator(operator: String) {
        this.operator.set(operator)
    }

    fun mapReduce(size: Long) {
        val time = System.currentTimeMillis()
        val last = lastPrint.get()
        val calls = mapReduce.incrementAndGet()
        val currentSize = mapReduceSize.addAndGet(size)
        //CAS ensures only one thread will actually print something.
        //We might lose one or two calls, but that really doesn't matter.
        if (time > last + printInterval && lastPrint.compareAndSet(last, time)) {
            mapReduce.set(0)
            mapReduceSize.set(0)
            output?.println("Map-Reduce: $calls calls. (avr. size: ${currentSize/calls})")
            output?.println("Verification: ${operator.get()}")
        }
        //update global stats
        totalMapReduce.incrementAndGet()
        totalMapReduceSize.addAndGet(size)
    }

    fun printGlobal() {
        val total = totalMapReduce.get()
        val totalSize = totalMapReduceSize.get()
        output?.println("Total Map-Reduce calls: $total")
        output?.println("Average call size: ${totalSize/Math.max(1, total).toDouble()}")
    }

}