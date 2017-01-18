package com.github.sybila.checker


/**
 * All communication actions are global synchronization events.
 */
interface Channel<Params : Any> : Partition<Params> {

    /**
     * Send data to each receiver in the array (list on index 0 goes to partition 0, etc.).
     *
     * Afterwards receive data from all senders and group them into one list.
     *
     * If no data has been sent by ANY of the partitions (all arrays were full of nulls),
     * the returned value will be null. (used for termination detection)
     * If at least one process sent some data, but not to this partition, returned value will be an empty list.
     *
     * @Contract array.size == partitionCount
     */
    fun mapReduce(outgoing: Array<List<Pair<Int, Params>>?>): List<Pair<Int, Params>>?

}