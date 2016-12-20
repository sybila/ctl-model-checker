package com.github.sybila.checker.new

import java.nio.ByteBuffer
import java.util.*

private val buffers = ArrayList<ByteBuffer>()

fun obtainBuffer(capacity: Int): ByteBuffer {
    synchronized(buffers) {
        var bestCandidate = -1
        var bestCapacity = Int.MAX_VALUE
        for (i in buffers.indices) {
            val bufferCapacity = buffers[i].capacity()
            if (bufferCapacity >= capacity && bufferCapacity <= bestCapacity) {
                bestCandidate = i
                bestCapacity = bufferCapacity
            }
        }
        if (bestCandidate >= 0) {
            return buffers.removeAt(bestCandidate)
        }
    }
    return ByteBuffer.allocate(capacity)
}

fun recycleBuffer(buffer: ByteBuffer) {
    synchronized(buffers) {
        buffers.add(buffer)
    }
}