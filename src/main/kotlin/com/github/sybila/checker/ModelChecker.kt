package com.github.sybila.checker

import com.github.daemontus.egholm.logger.lFine
import com.github.daemontus.egholm.logger.lFinest
import com.github.daemontus.egholm.logger.lInfo
import com.github.sybila.ctl.Atom
import com.github.sybila.ctl.Formula
import com.github.sybila.ctl.Op
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class ModelChecker<N: Node, C: Colors<C>>(
        fragment: KripkeFragment<N, C>,
        private val queueFactory: JobQueue.Factory<N, C>,
        private val logger: Logger = Logger.getLogger(ModelChecker::class.java.canonicalName).apply {
            level = Level.OFF
        }
):
        KripkeFragment<N, C> by fragment, WithStats
{

    //Time spent in state space generator successor/predecessor methods
    private var timeInGenerator = 0L
    //Time spent verifying formulas (not necessarily working - can just sleep)
    private var verificationTime = 0L

    /**
     * Push given colors to all predecessors of given node as jobs.
     */
    private val pushBack: N.(C) -> List<Job<N, C>> = { border ->
        val start = System.nanoTime()
        val predecessors = this.predecessors()
        timeInGenerator += System.nanoTime() - start
        predecessors.entries.map {
            Job(this, it.key, it.value intersect border)
        }
    }

    private val results: MutableMap<Formula, Nodes<N, C>> = HashMap()

    fun verify(f: Formula): Nodes<N, C> {
        logger.lInfo { "Started processing formula $f"}
        val start = System.nanoTime()
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
                    Op.ALL_UNTIL -> checkAllUntil(f)
                    else -> throw IllegalArgumentException("Unsupported operator: ${f.operator}")
                }
            }
        }
        logger.lInfo { "Finished processing formula $f"}
        verificationTime += System.nanoTime() - start
        return results[f]!!
    }

    private fun checkNegation(f: Formula): Nodes<N, C> = allNodes() subtract verify(f[0])

    private fun checkAnd(f: Formula): Nodes<N, C> = verify(f[0]) intersect verify(f[1])

    private fun checkOr(f: Formula): Nodes<N, C> = verify(f[0]) union verify(f[1])

    private fun checkExistNext(f: Formula): Nodes<N, C> {

        val phi = verify(f[0])

        logger.lFine { "Found ${phi.entries.count()} initial states."}

        val result = HashMap<N, C>().toMutableNodes(phi.emptyColors)

        val initial = phi.entries.flatMap { it.key.pushBack(it.value) }

        queueFactory.createNew(initial) {
            result.putOrUnion(it.target, it.colors)
            logger.lFinest { "Add ${it.colors} to ${it.target}" }
        }.waitForTermination()

        logger.lFine { "Results contain ${results.entries.size} entries." }

        return result.toNodes() //defensive copy, maybe redundant
    }

    private fun checkExistUntil(f: Formula): Nodes<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])

        logger.lFine { "Found ${phi_1.entries.count()} and ${phi_2.entries.count()} initial states." }

        val result = phi_2.toMutableNodes() //this is the "initial step" where you mark as valid only the nodes where phi_2 holds.

        val initial = phi_2.entries.flatMap { it.key.pushBack(it.value) }

        queueFactory.createNew(initial) {
            val target = it.target
            val colors = it.colors intersect phi_1[target]
            logger.lFinest { "Add $colors to $target - pushed from ${it.source}" }
            if (result.putOrUnion(target, colors)) {
                target.pushBack(colors).map { post(it) }
            }
        }.waitForTermination()

        logger.lFine { "Results contain ${results.entries.size} entries." }

        return result.toNodes()
    }

    private fun checkAllUntil(f: Formula): Nodes<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])

        logger.lFine { "Found ${phi_1.entries.count()} and ${phi_2.entries.count()} initial states." }

        val result = phi_2.toMutableNodes() //this is the "initial step" where you mark as valid only the nodes where phi_2 holds.

        //Remembers which successors have been covered by explored edges (successors are lazily initialized)
        //Algorithm modifies the contents to satisfy following invariant:
        //uncoveredEdges(x,y) = { c such that there is an edge into y and !(phi_2 or (phi_1 AU phi_2)) holds in y }
        //Note: Maybe we could avoid this if we also allowed results for border states in results map.
        val uncoveredEdges = HashMap<N, MutableMap<N, C>>()

        val initial = phi_2.entries.flatMap { it.key.pushBack(it.value) }

        queueFactory.createNew(initial) {
            val uncoveredSuccessors = synchronized(uncoveredEdges) {
                val start = System.nanoTime()
                val successors = it.target.successors()
                timeInGenerator += System.nanoTime() - start
                if (it.target !in uncoveredEdges) uncoveredEdges[it.target] = successors.toMutableMap()
                uncoveredEdges[it.target]!!
            }
            val validColors = synchronized(uncoveredSuccessors) {
                //cover pushed edge
                //Would this be reasonably faster if we removed empty sets from map completely?
                uncoveredSuccessors[it.source] = uncoveredSuccessors[it.source]!! - it.colors
                //Compute what colors became covered by this change
                //Or should we cache results of this reduction?
                phi_1[it.target] intersect (it.colors - uncoveredSuccessors.values.reduce { a, b -> a union b })
            }
            logger.lFinest { "Add $validColors to ${it.target} - pushed from ${it.source}" }
            if (validColors.isNotEmpty() && result.putOrUnion(it.target, validColors)) { //if some colors survived all of this, mark them and push further
                it.target.pushBack(validColors).map { post(it) }
            }
        }.waitForTermination()

        logger.lFine { "Results contain ${results.entries.size} entries." }

        return result.toNodes()
    }

    override fun getStats(): Map<String, Any> {
        return mapOf(
                "Time in generator" to timeInGenerator,
                "Verification time" to verificationTime
        )
    }

    override fun resetStats() {
        timeInGenerator = 0L
        verificationTime = 0L
    }

}