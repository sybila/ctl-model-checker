package com.github.sybila.checker.channel

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Partition
import com.github.sybila.checker.new.Solver

class SingletonChannel<Params : Any>(
        partition: Partition<Params>
) : Channel<Params>, Partition<Params> by partition {

    override fun mapReduce(outgoing: Array<List<Pair<Int, Params>>?>, solver: Solver<Params>): List<Pair<Int, Params>>? {
        if (outgoing.size != 1 || outgoing[0]?.isNotEmpty() ?: false) {
            throw IllegalStateException("Trying to send messages using a singleton channel!")
        }
        return null
    }

}