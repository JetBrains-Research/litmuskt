package komem.litmus

import komem.litmus.barriers.Barrier
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerRunner : LitmusRunner() {

    @OptIn(ObsoleteWorkersApi::class, ExperimentalNativeApi::class)
    override fun <S> runTest(
        params: LitmusRunParams,
        test: LitmusTest<S>,
    ): LitmusResult {

        data class WorkerContext(
            val states: List<S>,
            val threadFunction: S.() -> Any?,
            val syncPeriod: Int,
            val barrier: Barrier,
        )

        val states = List(params.batchSize) { test.stateProducer() }
        val barrier = params.barrierProducer(test.threadCount)
        val outcomeFinalizer = test.outcomeFinalizer
        val workers = List(test.threadCount) { Worker.start() }

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
                test.threadFunctions[threadIndex],
                params.syncPeriod,
                barrier,
            )
            worker.execute(
                TransferMode.SAFE /* ignored */,
                { workerContext }
            ) { (states, threadFunction, syncPeriod, barrier) ->
                for (i in states.indices) {
                    if (i % syncPeriod == 0) barrier.await()
                    states[i].threadFunction()
                }
            }
            worker.requestTermination()
        }.forEach { it.result } // await all workers

        val outcomes = states.map { it.outcomeFinalizer() }
        assert(outcomes.size == params.batchSize)

        return outcomes.calcStats(test.outcomeSpec)
    }
}
