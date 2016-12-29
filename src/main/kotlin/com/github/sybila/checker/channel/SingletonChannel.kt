package com.github.sybila.checker.channel

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Partition

class SingletonChannel<Params : Any>(
        partition: Partition<Params>
) : Channel<Params>, Partition<Params> by partition {

    override fun mapReduce(outgoing: Array<List<Pair<Int, Params>>?>): List<Pair<Int, Params>>? {
        if (outgoing.size != 1 || outgoing[0]?.isNotEmpty() ?: false) {
            throw IllegalStateException("Trying to send messages using a singleton channel!")
        }
        return null
    }

}