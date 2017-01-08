package com.github.sybila.checker.shared.solver

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val solverCalls = AtomicLong()
private val lastSolverCall = AtomicLong(System.currentTimeMillis())

fun solverCalled() {
    val time = System.currentTimeMillis()
    val last = lastSolverCall.get()
    val calls = solverCalls.incrementAndGet()
    if (time > last + 2000 && lastSolverCall.compareAndSet(last, time)) {
        println("Solver calls: ${calls/2}/s")
        solverCalls.set(0)
    }
}