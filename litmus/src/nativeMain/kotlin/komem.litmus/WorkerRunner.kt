package komem.litmus

import komem.litmus.barriers.Barrier
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerRunner : LitmusRunner() {

    @OptIn(ObsoleteWorkersApi::class)
    override fun <S> startTest(
        params: LitmusRunParams,
        test: LitmusTest<S>,
    ): () -> LitmusResult {

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

        val futures = workers.mapIndexed { threadIndex, worker ->
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
        }

        return {
            futures.forEach { it.result } // await all results
            workers.forEach { it.requestTermination().result } // waits for all workers to stop
            val outcomes = states.map { it.outcomeFinalizer() }
            outcomes.calcStats(test.outcomeSpec)
        }
    }
}
