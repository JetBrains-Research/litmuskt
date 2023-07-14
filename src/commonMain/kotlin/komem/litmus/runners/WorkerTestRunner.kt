package komem.litmus.runners

import komem.litmus.LTDefinition
import komem.litmus.LitmusOutcome
import komem.litmus.RunParams
import komem.litmus.barriers.Barrier
import komem.litmus.getAffinityManager
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerTestRunner : LitmusTestRunner {

    @OptIn(ObsoleteWorkersApi::class, ExperimentalNativeApi::class)
    override fun <S> runTest(
        params: RunParams,
        test: LTDefinition<S>,
    ): List<LitmusOutcome> {

        data class WorkerContext(
            val states: List<S>,
            val threadOutcomes: MutableList<Any?>?,
            val threadFunction: S.() -> Any?,
            val syncPeriod: Int,
            val barrier: Barrier,
        )

        val states = List(params.batchSize) { test.stateProducer() }
        val barrier = params.barrierProducer(test.threadCount)
        val outcomeFinalizer = test.outcomeFinalizer

        val workers = List(test.threadCount) { Worker.start() }
        val outcomesLists = workers.map {
            if (outcomeFinalizer == null) MutableList<Any?>(params.batchSize) {} else null
        }

        workers.mapIndexed { threadIndex, worker ->
            params.affinityMap?.let { affinityMap ->
                getAffinityManager()?.run {
                    val cpuSet = affinityMap.allowedCores(threadIndex)
                    setAffinity(worker, cpuSet)
                    require(getAffinity(worker) == cpuSet) { "affinity setting failed" }
                }
            }
            val workerContext = WorkerContext(
                states,
                outcomesLists[threadIndex],
                test.threadFunctions[threadIndex],
                params.syncPeriod,
                barrier,
            )
            worker.execute(
                TransferMode.SAFE /* ignored */,
                { workerContext }
            ) { (states, threadOutcomes, threadFunction, syncPeriod, barrier) ->
                if (threadOutcomes != null) {
                    for (stateIdx in states.indices) {
                        if (stateIdx % syncPeriod == 0) barrier.wait()
                        threadOutcomes[stateIdx] = states[stateIdx].threadFunction()
                    }
                } else {
                    for (i in states.indices) {
                        if (i % syncPeriod == 0) barrier.wait()
                        states[i].threadFunction()
                    }
                }
                threadOutcomes
            }
            worker.requestTermination()
        }.forEach { it.result } // await all workers

        // TODO: this complex logic is defined in plain text (!) in LitmusTestDsl.kt (!)
        val outcomes = if (outcomeFinalizer == null) {
            val outcomesTransposed = MutableList(params.batchSize) { MutableList<Any?>(test.threadCount) { Unit } }
            repeat(params.batchSize) { stateIndex ->
                repeat(test.threadCount) { threadIndex ->
                    outcomesTransposed[stateIndex][threadIndex] = outcomesLists[threadIndex]!![stateIndex]
                }
            }
            if (outcomesTransposed.all { it.count { it != Unit } == 1 }) {
                outcomesTransposed.map { it.find { it != Unit } }
            } else outcomesTransposed
        } else {
            states.map { it.outcomeFinalizer() }
        }
        assert(outcomes.size == params.batchSize)

        return outcomes
    }
}
