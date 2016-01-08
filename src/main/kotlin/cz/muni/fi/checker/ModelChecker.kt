package cz.muni.fi.checker

import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import java.util.*

public class ModelChecker<N: Node, C: Colors<C>>(
        fragment: KripkeFragment<N, C>,
        private val jobQueues: JobQueue.Factory<N, C>
):
        KripkeFragment<N, C> by fragment
{

    private val pushBack: N.(C) -> List<Pair<N, C>> = { border ->
        this.predecessors().validEntries.map {
            Pair(it.key, it.value intersect border)
        }
    }

    private val results: MutableMap<Formula, Nodes<N, C>> = HashMap()

    public fun verify(f: Formula): Nodes<N, C> {
        if (f !in results) {
            results[f] = if (f is Atom) {
                validNodes(f)
            } else {
                when (f.operator) {
                    Op.NEGATION -> checkNegation(f)
                    Op.AND -> checkAnd(f)
                    Op.OR -> checkOr(f)
                    Op.EXISTS_NEXT -> checkExistNext(f)
                    Op.EXISTS_UNTIL -> checkExistUntil(f)
                    else -> throw IllegalArgumentException("Unsupported operator: ${f.operator}")
                }
            }
        }
        return results[f]!!
    }

    private fun checkNegation(f: Formula): Nodes<N, C> = allNodes() subtract verify(f[0])

    private fun checkAnd(f: Formula): Nodes<N, C> = verify(f[0]) intersect verify(f[1])

    private fun checkOr(f: Formula): Nodes<N, C> = verify(f[0]) union verify(f[1])

    private fun checkExistNext(f: Formula): Nodes<N, C> {

        val phi = verify(f[0])

        val result = HashMap<N, C>().toMutableNodes(phi.emptyColors)

        val predecessors = phi.validEntries
                .flatMap { it.key.pushBack(it.value) }
                .map { Job.EX(it.first, it.second) }

        jobQueues.createNew(predecessors, genericClass()) {
            result.putOrUnion(it.node, it.colors)
        }.waitForTermination()

        return result
    }

    private fun checkExistUntil(f: Formula): Nodes<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])

        val result = HashMap<N, C>().toMutableNodes(phi_1.emptyColors)

        val predecessors = phi_2.validEntries.flatMap { e ->
            val (node, colors) = e
            if (result.putOrUnion(node, colors)) {
                node.pushBack(colors).map { Job.EU(it.first, it.second) }
            } else listOf()
        }

        jobQueues.createNew(predecessors, genericClass()) {
            val (node, colors) = it
            if (result.putOrUnion(node, colors intersect phi_1[node])) {
                node.pushBack(colors).map { post(Job.EU(it.first, it.second)) }
            }
        }.waitForTermination()

        return result
    }

    private fun checkAllUntil(f: Formula): Nodes<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])
        val result = HashMap<N, C>().toMutableNodes(phi_1.emptyColors)

        //Remembers which successors have been covered by explored edges (successors are lazily initialized)
        //Algorithm modifies the contents to satisfy following invariant:
        //uncoveredEdges(x,y) = { c such that there is an edge into y and !(phi_2 or (phi_1 AU phi_2)) holds in y }
        //Note: Maybe we could avoid this if we also allowed results for border states in results map.
        /*val uncoveredEdges = HashMap<N, MutableMap<N, C>>()

        val jobQueue = SingleThreadJobQueue(
                messengers, partitionFunction,
                genericClass<Job.AU<N, C>>()
        ) {
            synchronized(uncoveredEdges) {  //Lazy init map with successors
                if (it.targetNode !in uncoveredEdges) uncoveredEdges[it.targetNode] = HashMap(it.targetNode.successors())
            }
            val uncoveredSuccessors = uncoveredEdges[it.targetNode]!!
            val validColors = synchronized(uncoveredSuccessors) {
                //cover pushed edge
                //Would this be reasonably faster if we removed empty sets from map completely?
                uncoveredSuccessors[it.sourceNode] == uncoveredSuccessors[it.sourceNode]!! - it.colors
                //Compute what colors became covered by this change
                //Or should we cache results of this reduction?
                phi_1.getOrDefault(it.targetNode) intersect (it.colors - uncoveredSuccessors.values().reduce { a, b -> a union b })
            }
            if (validColors.isNotEmpty()) { //if some colors survived all of this, mark them and push further
                val modified = synchronized(result) { result.putOrUnion(it.targetNode, validColors) }
                if (modified) pushBack(it.targetNode, validColors, this) { s, t, c -> Job.AU(s, t, c) }
            }
        }


        //Push from all nodes where phi_2 holds, but do not mark anything
        for ((node, colors) in phi_2) pushBack(node, colors, jobQueue) { s, t, c -> Job.AU(s, t, c) }

        //Update info about uncovered edges and if some colors become fully covered,
        //mark the target and push them further

        jobQueue.waitForTermination()*/
        return result
    }

}