@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

import barriers.Barrier
import barriers.BarrierProducer
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerTestRunner : LitmusTestRunner {

    private data class WorkerContext(
        val tests: List<BasicLitmusTest>,
        val syncPeriod: Int,
        val barrier: Barrier,
    )

    override fun runTest(
        batchSize: Int,
        parameters: LitmusTestParameters,
        testProducer: () -> BasicLitmusTest,
    ): LitmusResult {
        BasicLitmusTest.memShuffler = parameters.memShufflerProducer?.invoke()
        val actorFunctions: List<(BasicLitmusTest) -> Any?> = testProducer().overriddenActors()
        val testBatch = List(batchSize) { testProducer() }
        createFutures(
            testBatch,
            parameters.syncPeriod,
            parameters.barrierProducer,
            actorFunctions,
            parameters.affinityMap
        ).forEach { it.result }
        for (test in testBatch) test.arbiter()
        return collectOutcomes(testBatch)
    }

    override fun runTestParallel(
        batchSize: Int,
        parameters: LitmusTestParameters,
        testProducer: () -> BasicLitmusTest
    ): LitmusResult {
        BasicLitmusTest.memShuffler = parameters.memShufflerProducer?.invoke()
        val actorFunctions: List<(BasicLitmusTest) -> Any?> = testProducer().overriddenActors()

        val parallelFactor = cpuCount() / actorFunctions.size
        val testBatches = List(parallelFactor) { List(batchSize) { testProducer() } }
        List(parallelFactor) { i ->
            createFutures(
                testBatches[i],
                parameters.syncPeriod,
                parameters.barrierProducer,
                actorFunctions,
                null
            )
        }.flatten().map { it.result }
        val outcomes = testBatches
            .flatten()
            .onEach { it.arbiter() }
            .let { collectOutcomes(it) }
        return outcomes
    }

    private fun createFutures(
        testBatch: List<BasicLitmusTest>,
        syncPeriod: Int,
        barrierProducer: BarrierProducer,
        actorFunctions: List<(BasicLitmusTest) -> Any?>,
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

    private fun collectOutcomes(testBatch: List<BasicLitmusTest>): List<OutcomeInfo> {
        val outcomeSetup = testBatch.first().getOutcomeSetup()
        return testBatch
            .map { it.outcome }
            .groupingBy { it }
            .eachCount()
            .map { (outcome, count) ->
                OutcomeInfo(outcome, count.toLong(), outcomeSetup?.getType(outcome))
            }
    }
}
