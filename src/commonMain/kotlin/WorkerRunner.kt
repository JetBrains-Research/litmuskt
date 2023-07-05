@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

import barriers.Barrier
import barriers.BarrierProducer
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerRunner : LitmusRunner {

    private data class WorkerContext(
        val tests: List<LitmusTest>,
        val syncPeriod: Int,
        val barrier: Barrier,
    )

    override fun runTest(
        options: LitmusOptions,
        testProducer: () -> LitmusTest,
    ): List<LitmusOutcome> {
        LitmusTest.memShuffler = options.memShufflerProducer?.invoke()
        val actorFunctions: List<(LitmusTest) -> Any?> = testProducer().overriddenActors()
        val testBatch = List(options.batchSize) { testProducer() }
        createFutures(
            testBatch,
            options.syncPeriod,
            options.barrierProducer,
            actorFunctions,
            options.affinityMap
        ).forEach { it.result }
        for (test in testBatch)
            test.arbiter()
//        val outcomeSetup = testBatch.first().getOutcomeSetup()
        return testBatch.map { it.outcome }
    }

    private fun createFutures(
        testBatch: List<LitmusTest>,
        syncPeriod: Int,
        barrierProducer: BarrierProducer,
        actorFunctions: List<(LitmusTest) -> Any?>,
        affinityMap: AffinityMap?
    ): List<Future<*>> {
        val workerContext = WorkerContext(
            testBatch,
            syncPeriod,
            barrierProducer(actorFunctions.size)
        )
        val futures = actorFunctions.mapIndexed { i, actorFun ->
            val worker = Worker.start()

            if (affinityMap != null) {
                getAffinityManager()?.run {
                    val cpuSet = affinityMap.allowedCores(i)
                    setAffinity(worker, cpuSet)
                    require(getAffinity(worker) == cpuSet) { "affinity setting failed" }
                }
            }

            worker.execute(
                TransferMode.SAFE /* ignored */,
                { actorFun to workerContext }
            ) { (actorFun, workerContext) ->
                workerContext.apply {
                    var cnt = 0
                    for (test in tests) {
                        if (cnt == this.syncPeriod) {
                            cnt = 0
                            barrier.wait()
                        }
                        actorFun(test)
                        cnt++
                    }
                }
            }
            return@mapIndexed worker.requestTermination()
        }
        return futures
    }

}
