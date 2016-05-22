package com.github.sybila.checker.uctl

import com.github.sybila.checker.*
import com.github.sybila.ctl.Atom
import com.github.sybila.ctl.False
import com.github.sybila.ctl.Formula
import com.github.sybila.ctl.Op
import java.util.*

class UModelChecker<N: Node, C: Colors<C>>(
        fragment: DirectedKripkeFragment<N, C>,
        private val emptyColors: C,
        private val fullColors: C
):
        DirectedKripkeFragment<N, C> by fragment
{

    private val results: MutableMap<Pair<UFormula, Map<String, Pair<N, C>>>, Nodes<N, C>> = HashMap()

    fun verify(f: UFormula, vars: Map<String, Pair<N, C>>): Nodes<N, C> {
        return when (f) {
                is UProposition -> validNodes(f.proposition)
                is UNot -> allNodes() - verify(f.formula, vars)
                is UAnd -> verify(f.left, vars) intersect verify(f.right, vars)
                is UName -> {
                    nodesOf(emptyColors, vars[f.name] ?: throw IllegalStateException("Unknown name ${f.name}"))
                }
                is UEU -> checkExistUntil(f, vars)
                is UAU -> checkAllUntil(f, vars)
                is UEX -> {
                    val result = HashMap<N, C>().toMutableNodes(emptyColors)
                    val inner = verify(f.inner, vars)
                    for (entry in allNodes().entries) {
                        for ((succ, sCol) in next(entry.key, !f.forward).entries) {
                            val push = sCol intersect inner[succ]
                            if (push.isNotEmpty() && checkTransition(entry.key, succ, f.direction)) {
                                result.putOrUnion(entry.key, push)
                            }
                        }
                    }
                    result.toNodes()
                }
                is UAX -> {
                    verify(UNot(UEX(true, UNot(f.inner), f.direction)), vars)
                    /*val result = HashMap<N, C>().toMutableNodes(emptyColors)
                    val inner = verify(f.inner, vars)
                    for (entry in allNodes().entries) {
                        val valid = fullColors
                        for ((succ, sCol) in next(entry.key, !f.forward).entries) {
                            val push = sCol intersect inner[succ]
                            if (push.isNotEmpty() && checkTransition(entry.key, succ, f.direction)) {
                                result.putOrUnion(entry.key, push)
                            }
                        }
                    }
                    result.toNodes()*/
                }
                is UBind -> {
                    val result = HashMap<N, C>().toMutableNodes(emptyColors)
                    for (entry in allNodes().entries) {
                        val inner = verify(f.inner, vars + Pair(f.name, entry.toPair()))
                        if (inner[entry.key].isNotEmpty()) {
                            result.putOrUnion(entry.key, inner[entry.key])
                        }
                    }
                    result.toNodes()
                }
                is UAt -> {
                    val inner = verify(f.inner, vars)
                    val state = vars[f.name] ?: throw IllegalStateException("Unknown name ${f.name}")

                    val result = HashMap<N, C>().toMutableNodes(emptyColors)
                    if (inner[state.first].isNotEmpty()) {
                        for (entry in allNodes().entries) {
                            result.putOrUnion(entry.key, inner[state.first])
                        }
                    }
                    result.toNodes()
                }
                is UExists -> {
                    var result = HashMap<N, C>().toNodes(emptyColors)
                    var counter = 0
                    for (entry in allNodes().entries) {
                        counter += 1
                        if (counter % 1000 == 1) println("Counter: $counter")
                        val inner = verify(f.inner, vars + Pair(f.name, entry.toPair()))
                        result += inner
                    }
                    result
                }
                else -> throw IllegalStateException("Unknown formula: $f")
            }
    }

    private fun next(state: N, forward: Boolean): Nodes<N, C> {
        return if (forward) {
            state.predecessors()
        } else {
            state.successors()
        }
    }

    private fun checkExistUntil(f: UEU, vars: Map<String, Pair<N, C>>) : Nodes<N, C> {

        val phi_1 = verify(f.path, vars)
        val phi_2 = verify(f.reach, vars)

        val results = HashMap<N, C>().toMutableNodes(emptyColors)

        val queue = HashMap<N, C>()

        fun enqueue(n: N, c: C) {
            if (n !in queue) {
                queue[n] = c
            } else {
                queue[n] = queue[n]!! union c
            }
        }

        fun pick(): Pair<N, C> {
            val (n, c) = queue.iterator().next();
            queue.remove(n)
            return Pair(n, c)
        }

        for (entry in phi_2.entries) {
            for ((succ, col) in next(entry.key, !f.forward).entries) {
                val active = entry.value.intersect(col)
                if (active.isNotEmpty() && checkTransition(entry.key, succ, f.reachDirection)) {
                    results.putOrUnion(entry.key, active)
                    for ((pred, pCol) in next(entry.key, f.forward).entries) {
                        val push = entry.value.intersect(pCol)
                        if (push.isNotEmpty() && checkTransition(pred, entry.key, f.pathDirection)) {
                            enqueue(entry.key, push)
                        }
                    }
                }
            }
        }

        while (queue.isNotEmpty()) {
            val (n, c) = pick()
            val andPhi_1 = c intersect phi_1[n]
            if (andPhi_1.isNotEmpty() && results.putOrUnion(n, andPhi_1)) {
                for ((pred, pCol) in next(n, f.forward).entries) {
                    val push = andPhi_1.intersect(pCol)
                    if (push.isNotEmpty() && checkTransition(pred, n, f.pathDirection)) {
                        enqueue(pred, push)
                    }
                }
            }
        }

        return results.toNodes()
    }

    private fun checkAllUntil(f: UAU, vars: Map<String, Pair<N, C>>) : Nodes<N, C> {

        val phi_1 = verify(f.path, vars)
        val phi_2 = verify(f.reach, vars)

        val results = HashMap<N, C>().toMutableNodes(emptyColors)

        //Remembers which successors have been covered by explored edges (successors are lazily initialized)
        //Algorithm modifies the contents to satisfy following invariant:
        //uncoveredEdges(x,y) = { c such that there is an edge into y and !(phi_2 or (phi_1 AU phi_2)) holds in y }
        //Note: Maybe we could avoid this if we also allowed results for border states in results map.
        val uncoveredEdges = HashMap<N, MutableMap<N, C>>()

        val queue = HashMap<Pair<N, N>, C>()

        fun enqueue(n: Pair<N, N>, c: C) {
            if (n !in queue) {
                queue[n] = c
            } else {
                queue[n] = queue[n]!! union c
            }
        }

        fun pick(): Pair<Pair<N, N>, C> {
            val (n, c) = queue.iterator().next();
            queue.remove(n)
            return Pair(n, c)
        }

        for (entry in phi_2.entries) {
            var valid = fullColors
            //we must gather all colors for which we have only a successor in the correct direction
            for ((succ, col) in next(entry.key, !f.forward).entries) {
                val sCol = entry.value.intersect(col)
                if (sCol.isNotEmpty() && !checkTransition(entry.key, succ, f.reachDirection)) {
                    valid -= sCol
                }
            }
            if (valid.isNotEmpty() && results.putOrUnion(entry.key, valid)) {
                for ((pred, pCol) in next(entry.key, f.forward).entries) {
                    val push = valid.intersect(pCol)
                    if (push.isNotEmpty() && checkTransition(pred, entry.key, f.pathDirection)) {
                        enqueue(Pair(pred, entry.key), push)
                    }
                }
            }
        }

        while (queue.isNotEmpty()) {
            val (pair, c) = pick()
            val (current, succ) = pair
            val uncoveredSuccessors = uncoveredEdges.getOrPut(current) {
                next(current, !f.forward).toMutableMap()
            }

            //cover pushed edge
            //Would this be reasonably faster if we removed empty sets from map completely?
            uncoveredSuccessors[succ] = uncoveredSuccessors[succ]!! - c
            //Compute what colors became covered by this change
            //Or should we cache results of this reduction?
            val becameUncovered = phi_1[current] intersect (c - uncoveredSuccessors.values.reduce { a, b -> a union b })

            if (becameUncovered.isNotEmpty() && results.putOrUnion(current, becameUncovered)) {
                for ((pred, pCol) in next(current, f.forward).entries) {
                    val push = becameUncovered.intersect(pCol)
                    if (push.isNotEmpty() && checkTransition(pred, current, f.pathDirection)) {
                        enqueue(Pair(pred, current), push)
                    }
                }
            }
        }

        return results.toNodes()

    }

}