package com.github.sybila.checker.new

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

interface Comm<Colors> {

    val size: Int

    /**
     * Return true is some messages were exchanged during the synchronization.
     */
    fun synchronize(fixPoint: FixPoint<Colors>): Boolean

}

class SharedMemComm<Colors>(
        override val size: Int
) : Comm<Colors> {

    private val barrier = CyclicBarrier(size)
    private val waiting = arrayOfNulls<FixPoint<Colors>>(size)
    private val done = AtomicInteger(0)

    override fun synchronize(fixPoint: FixPoint<Colors>): Boolean {
        waiting[fixPoint.id] = fixPoint
        //println("${fixPoint.id} started waiting")
        barrier.await()
        //make local copy
        val fixPoints = waiting.toList().requireNoNulls()
        //exchange messages
        try {
            fixPoint.map(fixPoints)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        //println("${fixPoint.id} map complete")
        done.set(0)
        barrier.await()
        try {
            if (fixPoint.reduce()) done.incrementAndGet()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        //println("${fixPoint.id} reduce complete")
        barrier.await()
        return done.get() != 0
    }

}

class NoComm<Colors> : Comm<Colors> {
    override val size: Int = 1
    override fun synchronize(fixPoint: FixPoint<Colors>): Boolean = false
}