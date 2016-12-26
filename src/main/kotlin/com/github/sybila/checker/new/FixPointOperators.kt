package com.github.sybila.checker.new

import com.github.sybila.huctl.DirectionFormula
import java.util.*


/**
 *
 * Fix point algorithms:
 *
 *  BigFixPoint(init) {
 *      add(s)
 *      onAdded(s)
 *  }
 *
 *  SmallFixPoint(init) {
 *      remove(s)
 *      onRemoved(s)
 *  }
 *
 *  EF: BigFixPoint
 *      onAdded s:
 *          for p in s.predecessors:
 *              add(p)
 *              if p is remote: sync(p, p.owner)
 *
 *  AG: SmallFixPoint
 *      onRemoved s:
 *          for p in s.predecessors:
 *              remove(p)
 *              if p is remote: sync(p, p.owner)
 *
 *  AF: BigFixPoint
 *      onAdded s:
 *          for p in s.predecessors:
 *              if (p is remote) sync(s, p.owner)
 *              else if (p is covered) add(p)
 *
 *  EG: SmallFixPoint
 *      onRemoved s:
 *          for p in s.predecessors:
 *              if (p is remote) sync(s, p.owner)
 *              else if (p !is covered) remove(p)
 *
 */

//Note: update functions handle true/false colors so we don't have to check it before calling

class EX<Colors>(
        timeFlow: Boolean,
        direction: DirectionFormula,
        initial: StateMap<Colors>,
        comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : FixPoint<Colors>(comm, solver, fragment) {

    init {
        for (state in initial) {
            val value = initial[state]
            for ((predecessor, dir, bound) in step(state, !timeFlow)) {
                if (direction.eval(dir)) {
                    if (update(predecessor, value and bound) && predecessor.owner() != id) {
                        sync(predecessor, predecessor.owner())
                    }
                }
            }
        }
    }

    override fun onUpdate(state: Int, value: Colors) { }    //do nothing

    override fun asStateMap(): StateMap<Colors> = localData.asStateMap(ff)

}

class AX<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        initial: StateMap<Colors>,
        comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : FixPoint<Colors>(comm, solver, fragment) {

    private val covered = HashMap<Int, Colors>()

    init {
        for (state in initial) {
            update(state, initial[state])
            for (t in step(state, !timeFlow)) {
                if (direction.eval(t.direction) && t.target.owner() != id) {
                    sync(state, t.target.owner())
                }
            }
        }
    }

    override fun onUpdate(state: Int, value: Colors) {
        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            if (predecessor.owner() != id) continue

            val witness = step(predecessor, timeFlow).asSequence().fold(value and bound) { a, t ->
                val (successor, sDir, sBound) = t
                val sValue = get(successor)
                val add = if (!direction.eval(sDir)) ff else (sValue and sBound)
                a and add
            }

            covered[predecessor] = (covered[predecessor] ?: ff) or witness
        }
    }

    override fun asStateMap(): StateMap<Colors> = covered.asStateMap(ff)

}

class EF<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        initial: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : GreatestFixPoint<Colors>(initial, comm, solver, fragment) {

    //Invariant: added states are valid.
    //Induction proof: state is added only if there is a successor where EF also holds under suitable direction.
    //Maximality: If EF holds at s but s is not marked, none of s|dir successor has been marked.
    override fun onAdded(state: Int, value: Colors) {
        if (state.owner() != id) return //stop on border

        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            val witness = value and bound
            if (direction.eval(dir)) {
                if (add(predecessor, witness) && predecessor.owner() != id) {
                    sync(predecessor, predecessor.owner())
                }
            }
        }
    }

}

class EU<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        private val path: StateMap<Colors>,
        initial: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : GreatestFixPoint<Colors>(initial, comm, solver, fragment) {

    //Invariant: added states are valid.
    //Induction proof: state is added only if there is a successor where EF also holds under suitable direction.
    //Maximality: If EF holds at s but s is not marked, none of s|dir successor has been marked.
    override fun onAdded(state: Int, value: Colors) {
        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            if (predecessor.owner() != id && state.owner() == id) {
                sync(state, predecessor.owner())
            } else if (predecessor.owner() == id) {
                val witness = value and bound and path[predecessor]
                if (direction.eval(dir)) {
                    if (add(predecessor, witness) && predecessor.owner() != id) {
                        sync(predecessor, predecessor.owner())
                    }
                }
            }
        }
    }

}

class AG<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        initial: StateMap<Colors>, all: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : SmallestFixPoint<Colors>(initial, all, comm, solver, fragment) {

    //Invariant: removed states are invalid.
    //Induction proof: state is removed when a) there is a transition that !dir b) there is a transition that sat. dir but !succ
    //Minimality: All states are checked at least once. Dir condition doesn't change, so it is handled the first time.
    //The rest holds by induction: If AG holds, no successor has been removed.
    override fun onRemoved(state: Int, value: Colors) {
        if (state.owner() != id) return

        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            val counterExample = if (!direction.eval(dir)) bound else (value and bound)
            if (remove(predecessor, counterExample) && predecessor.owner() != id) {
                sync(predecessor, predecessor.owner())
            }
        }
    }

}

class AF<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        initial: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : GreatestFixPoint<Colors>(initial, comm, solver, fragment) {

    //Invariant: added states are valid.
    //Induction proof: state is added only if all successor satisfy AF under suitable direction.
    //Maximality: If some state is not marked, then either some of it's successors is not marked or
    //there is a direction problem.
    override fun onAdded(state: Int, value: Colors) {
        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            if (predecessor.owner() != id && state.owner() == id) {
                sync(state, predecessor.owner())
            } else if (predecessor.owner() == id) {
                if (direction.eval(dir)) {  //otherwise can't propagate anything along this edge
                    //TODO: This can be done faster if we cache the results
                    val witness = step(predecessor, timeFlow).asSequence().fold(value and bound) { a, t ->
                        val (successor, sDir, sBound) = t
                        val sValue = get(successor)
                        val add = if (!direction.eval(sDir)) ff else (sValue and sBound)
                        a and add
                    }
                    add(predecessor, witness)   //we own the predecessor, no need to sync
                }
            }
        }
    }

}

class AU<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        private val path: StateMap<Colors>,
        initial: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : GreatestFixPoint<Colors>(initial, comm, solver, fragment) {

    //Invariant: added states are valid.
    //Induction proof: state is added only if all successor satisfy AF under suitable direction.
    //Maximality: If some state is not marked, then either some of it's successors is not marked or
    //there is a direction problem.
    override fun onAdded(state: Int, value: Colors) {
        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            if (predecessor.owner() != id && state.owner() == id) {
                sync(state, predecessor.owner())
            } else if (predecessor.owner() == id) {
                if (direction.eval(dir)) {  //otherwise can't propagate anything along this edge
                    //TODO: This can be done faster if we cache the results
                    val witness = step(predecessor, timeFlow).asSequence().fold(value and bound) { a, t ->
                        val (successor, sDir, sBound) = t
                        val sValue = get(successor)
                        val add = if (!direction.eval(sDir)) ff else (sValue and sBound)
                        a and add
                    }
                    add(predecessor, witness and path[predecessor])   //we own the predecessor, no need to sync
                }
            }
        }
    }

}

class EG<Colors>(
        private val timeFlow: Boolean,
        private val direction: DirectionFormula,
        initial: StateMap<Colors>, all: StateMap<Colors>, comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : SmallestFixPoint<Colors>(initial, all, comm, solver, fragment) {

    //Invariant: removed states are invalid.
    //Induction proof: state is removed when all successor that satisfy direction condition are removed
    //Minimality: All states are checked at least once. This will handle direction deadlocks. Rest is induction.
    override fun onRemoved(state: Int, value: Colors) {
        for ((predecessor, dir, bound) in step(state, !timeFlow)) {
            if (predecessor.owner() != id && state.owner() == id) {
                sync(state, predecessor.owner())
            } else if (predecessor.owner() == id) {
                //TODO: This can be done faster if we cache the results
                val counterExample = step(predecessor, timeFlow).asSequence().fold(value and bound) { a, t ->
                    val (successor, sDir, sBound) = t
                    val sValue = get(successor)
                    val remove = if (!direction.eval(sDir)) ff else (sValue and sBound)
                    a and remove
                }
                remove(predecessor, counterExample)
            }
        }
    }

}