package com.github.sybila.checker.operator

import com.github.sybila.checker.StateMap

internal fun <Params : Any> List<StateMap<Params>>.prepareTransmission(partitionId: Int): Array<List<Pair<Int, Params>>?>
        = this  .mapIndexed { i, map -> if (i == partitionId) null else map.entries().asSequence().toList() }
        .map { if (it?.isEmpty() ?: true) null else it }.toTypedArray()

internal fun <Params : Any> List<StateMap<Params>>.prepareFilteredTransmission(
        partitionId: Int, include: Set<Int>
): Array<List<Pair<Int, Params>>?>
        = this  .mapIndexed { i, map ->
    if (i == partitionId) null else map.entries().asSequence().filter { it.first in include }.toList()
}
        .map { if (it?.isEmpty() ?: true) null else it }.toTypedArray()
