package com.github.sybila.model

import com.github.sybila.collection.GenericStateMap
import com.github.sybila.collection.GenericStateSet
import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateSet
import com.github.sybila.huctl.DirFormula
import com.github.sybila.huctl.Formula
import java.util.*

/**
 * A generic implementation of [TransitionSystem] based on explicitly listed transitions and atomic propositions.
 */
class GenericTransitionSystem<S: Any, P: Any>(
        TT: P,
        transitions: Sequence<Pair<Pair<S, S>, Pair<P, DirFormula>>>,
        atomicPropositions: Sequence<Pair<Formula, StateMap<S, P>>>
) : TransitionSystem<S, P> {

    private val _states = HashSet<S>()
    private val _transitions: HashMap<Pair<S, S>, Pair<P, DirFormula>> = HashMap()
    private val _propositions: HashMap<Formula, StateMap<S, P>> = HashMap()

    private val _successors = HashMap<S, ArrayList<S>>()
    private val _predecessors = HashMap<S, ArrayList<S>>()

    init {
        transitions.forEach { (states, bounds) ->
            val (start, end) = states
            _states.add(start)
            _states.add(end)
            _successors.getList(start).add(end)
            _predecessors.getList(end).add(start)
            if (states in _transitions) throw IllegalArgumentException("Duplicate transition $start -> $end")
            _transitions[states] = bounds
        }
        atomicPropositions.forEach { (prop, map) ->
            map.states.forEach {
                if (it !in _states) throw IllegalArgumentException("Unknown state $it in proposition $prop")
            }
            if (prop in _propositions) throw IllegalArgumentException("Duplicate proposition $prop")
            _propositions[prop] = map
        }
    }

    override val states: StateSet<S> = GenericStateSet(_states)
    override val universe: StateMap<S, P> = GenericStateMap(_states.map { it to TT }.toMap())

    override fun S.successors(time: Boolean): Iterable<S> = (if (time) _successors else _predecessors)[this] ?: emptyList()

    override fun transitionBound(start: S, end: S): P? = _transitions[start to end]?.first

    override fun transitionDirection(start: S, end: S): DirFormula? = _transitions[start to end]?.second

    override fun makeProposition(formula: Formula): StateMap<S, P>
            = _propositions[formula] ?: throw IllegalArgumentException("Unknown proposition $formula")

    private fun HashMap<S, ArrayList<S>>.getList(state: S) = this.computeIfAbsent(state) { ArrayList() }
}