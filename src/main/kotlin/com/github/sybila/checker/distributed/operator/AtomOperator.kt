package com.github.sybila.checker.distributed.operator

import com.github.sybila.checker.distributed.Operator
import com.github.sybila.checker.distributed.Partition
import com.github.sybila.checker.distributed.StateMap
import com.github.sybila.huctl.Formula

class FalseOperator<out Params : Any>(
        partition: Partition<Params>
) : Operator<Params> {

    private val value
            = partition.run { emptyStateMap() }

    override fun compute(): StateMap<Params> = value
}

class TrueOperator<out Params : Any>(
        partition: Partition<Params>
) : Operator<Params> {

    private val value: StateMap<Params>
            = partition.run { (0 until stateCount).asStateMap(tt).restrictToPartition() }

    override fun compute(): StateMap<Params> = value

}

class ReferenceOperator<out Params : Any>(
        state: Int,
        partition: Partition<Params>
) : Operator<Params> {

    private val value = partition.run {
        if (state in this) state.asStateMap(tt) else emptyStateMap()
    }

    override fun compute(): StateMap<Params> = value

}

class FloatOperator<out Params : Any>(
        private val float: Formula.Atom.Float,
        private val partition: Partition<Params>
) : Operator<Params> {

    private val value: StateMap<Params> by lazy(LazyThreadSafetyMode.NONE) {
        partition.run { float.eval().restrictToPartition() }
    }

    override fun compute(): StateMap<Params> = value

}

class TransitionOperator<out Params : Any>(
        private val transition: Formula.Atom.Transition,
        private val partition: Partition<Params>
) : Operator<Params> {

    private val value: StateMap<Params> by lazy(LazyThreadSafetyMode.NONE) {
        partition.run { transition.eval().restrictToPartition() }
    }

    override fun compute(): StateMap<Params> = value

}