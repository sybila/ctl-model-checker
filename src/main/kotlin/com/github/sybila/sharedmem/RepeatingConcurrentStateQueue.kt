package com.github.sybila.sharedmem

import java.util.concurrent.atomic.AtomicIntegerArray

class RepeatingConcurrentStateQueue(
        private val stateCount: Int
) {

    private val set = AtomicIntegerArray((stateCount / 31) + 1)

    fun set(state: Int) {
        set.accumulateAndGet(state / 31, 1.shl(state % 31)) { value, mask -> value or mask }
    }

    fun next(from: Int): Int {
        for (s in from until stateCount) {
            var found = true
            do {
                val block = set.get(s / 31)
                if (block.shr(s%31).and(1) == 0) {
                    // state is not set, go to next
                    found = false
                    break
                }
                val newBlock = 1.shl(s%31).xor(block)
            } while (!set.compareAndSet(s/31, block, newBlock))
            if (found) return s
        }
        return -1
    }

}