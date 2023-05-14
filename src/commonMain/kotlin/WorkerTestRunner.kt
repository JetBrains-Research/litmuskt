import barriers.Barrier
import barriers.BarrierProducer
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerTestRunner : LitmusTestRunner {

    override var cpuCoreCount = 12 // TODO: proper interface

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

        val affinityMap = parameters.affinityMap
        if (affinityMap == null) {
            // parallel run
            val parallelFactor = cpuCoreCount / actorFunctions.size
            // TODO: pseudorandom affinity map
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
        } else {
            require(actorFunctions.size == affinityMap.size) { "affinity parameters don't match actors" }

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
                    val cpuSet = affinityMap[i]
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
                OutcomeInfo(outcome, count, outcomeSetup?.getType(outcome))
            }
    }
}
