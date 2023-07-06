@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

package komem.litmus.runners

import komem.litmus.*
import komem.litmus.barriers.Barrier
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerTestRunner : LitmusTestRunner {

    private data class WorkerContext(
        val tests: List<LitmusTest>,
        val syncPeriod: Int,
        val barrier: Barrier,
    )

    override fun runTest(
        params: RunningParams,
        testProducer: () -> LitmusTest,
    ): List<LitmusOutcome> {
        LitmusTest.memShuffler = params.memShufflerProducer?.invoke()

        val threadFunctions: List<(LitmusTest) -> Any?> = testProducer().overriddenThreads()
        val testBatch = List(params.batchSize) { testProducer() }
        val workerContext = WorkerContext(
            testBatch,
            params.syncPeriod,
            params.barrierProducer(threadFunctions.size)
        )
        val futures = threadFunctions.mapIndexed { i, threadFun ->
            val worker = Worker.start()

            val affinityMap = params.affinityMap
            if (affinityMap != null) {
                getAffinityManager()?.run {
                    val cpuSet = affinityMap.allowedCores(i)
                    setAffinity(worker, cpuSet)
                    require(getAffinity(worker) == cpuSet) { "affinity setting failed" }
                }
            }
            worker.execute(
                TransferMode.SAFE /* ignored */,
                { threadFun to workerContext }
            ) { (threadFun, workerContext) ->
                workerContext.apply {
                    var cnt = 0
                    for (test in tests) {
                        if (cnt == this.syncPeriod) {
                            cnt = 0
                            barrier.wait()
                        }
                        threadFun(test)
                        cnt++
                    }
                }
            }
            return@mapIndexed worker.requestTermination()
        }
        futures.forEach { it.result }

        for (test in testBatch)
            test.arbiter()
        return testBatch.map { it.outcome }
    }
}
