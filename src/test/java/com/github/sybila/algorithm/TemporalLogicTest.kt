package com.github.sybila.algorithm

import com.github.sybila.model.TransitionSystem
import com.github.sybila.model.increasingStateMap
import com.github.sybila.solver.Solver
import org.junit.Test
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.*

sealed class TemporalLogicTest(
        val model: ReachModel
) : TemporalLogic<BitSet>, TransitionSystem<BitSet> by model, BooleanLogic<BitSet> {

    override final val stateCount: Int = model.stateCount
    override val solver: Solver<BitSet> = model.solver
    protected val bound = 0 until stateCount

    @Test//(timeout = 10000)
    fun existsFuture() {
        // there is no way out of lower corner
        assertMapEquals(model.lowerCorner.block(), existsFinally(model.lowerCorner).block(), bound)

        val reach = solver.increasingStateMap(stateCount).apply {
            bound.forEach { this.increaseKey(it, model.stateColors(it)) }
        }

        assertMapEquals(
                model.upperCorner.disjunction(reach.asMono()).block(),
                existsFinally(model.upperCorner).block(), bound
        )

        assertMapEquals(
                model.border.disjunction(reach.asMono()).block(),
                existsFinally(model.border).block(), bound
        )

    }

    sealed class OneState(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(1,1)) {
        class Sequential : OneState(Schedulers.single())
        class Parallel : OneState(Schedulers.parallel())
    }

    sealed class TinyChain(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(1,2)) {
        class Sequential : TinyChain(Schedulers.single())
        class Parallel : TinyChain(Schedulers.parallel())
    }

    sealed class SmallChain(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(1,10)) {
        class Sequential : SmallChain(Schedulers.single())
        class Parallel : SmallChain(Schedulers.parallel())
    }

    sealed class LargeChain(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(1,1000)) {
        class Sequential : LargeChain(Schedulers.single())
        class Parallel : LargeChain(Schedulers.parallel())
    }

    sealed class SmallCube(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(2,2)) {
        class Sequential : SmallCube(Schedulers.single())
        class Parallel : SmallCube(Schedulers.parallel())
    }

    sealed class MediumCube(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(3,3)) {
        class Sequential : MediumCube(Schedulers.single())
        class Parallel : MediumCube(Schedulers.parallel())
    }

    sealed class LargeCube(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(5,5)) {
        class Sequential : LargeCube(Schedulers.single())
        class Parallel : LargeCube(Schedulers.parallel())
    }

    sealed class SmallAsymmetric1(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(2,4)) {
        class Sequential : SmallAsymmetric1(Schedulers.single())
        class Parallel : SmallAsymmetric1(Schedulers.parallel())
    }

    sealed class SmallAsymmetric2(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(4,2)) {
        class Sequential : SmallAsymmetric2(Schedulers.single())
        class Parallel : SmallAsymmetric2(Schedulers.parallel())
    }

    sealed class MediumAsymmetric1(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(3,6)) {
        class Sequential : MediumAsymmetric1(Schedulers.single())
        class Parallel : MediumAsymmetric1(Schedulers.parallel())
    }

    sealed class MediumAsymmetric2(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(6,4)) {
        class Sequential : MediumAsymmetric2(Schedulers.single())
        class Parallel : MediumAsymmetric2(Schedulers.parallel())
    }

    sealed class LargeAsymmetric1(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(6,5)) {
        class Sequential : LargeAsymmetric1(Schedulers.single())
        class Parallel : LargeAsymmetric1(Schedulers.parallel())
    }

    sealed class LargeAsymmetric2(override val scheduler: Scheduler) : TemporalLogicTest(ReachModel(5,7)) {
        class Sequential : LargeAsymmetric2(Schedulers.single())
        class Parallel : LargeAsymmetric2(Schedulers.parallel())
    }

}