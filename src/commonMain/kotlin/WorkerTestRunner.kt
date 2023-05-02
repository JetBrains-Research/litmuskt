import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

object WorkerTestRunner : LitmusTestRunner {
    private data class WorkerContext(
            val tests: List<BasicLitmusTest>,
            val syncPeriod: Int,
            val barrier: SpinBarrier,
    )

    override fun runTest(
            batchSize: Int,
            parameters: LitmusTestParameters,
            testProducer: () -> BasicLitmusTest
    ): LitmusResult {
        val actorFunctions: List<(BasicLitmusTest) -> Any?> = runBlocking { testProducer().overriddenActors() }
        require(actorFunctions.size == parameters.affinityMap.size) { "affinity parameters don't match actors" }
        BasicLitmusTest.memShuffler = parameters.memShufflerProducer?.invoke()
        val testBatch = List(batchSize) { testProducer() }
        val workerContext = WorkerContext(testBatch, parameters.syncPeriod, SpinBarrier(actorFunctions.size))
        actorFunctions.mapIndexed { i, actorFun ->
            val worker = Worker.start()

            getAffinityManager()?.run {
                val cpuSet = parameters.affinityMap[i]
                setAffinity(worker, cpuSet)
                require(getAffinity(worker) == cpuSet) { "affinity setting failed" }
            }

            worker.execute(
                    TransferMode.SAFE /* ignored */,
                    { actorFun to workerContext }
            ) { (actorFun, workerContext) ->
                runBlocking {
                    workerContext.apply {
                        var cnt = 0
                        for (test in tests) {
                            if (cnt == syncPeriod) {
                                cnt = 0
                                barrier.wait()
                            }
                            actorFun(test)
                            cnt++
                        }
                    }
                }
            }
            worker.requestTermination()
        }.forEach { it.result }
        for (test in testBatch) test.arbiter()
        val outcomeSetup = testBatch.first().getOutcomeSetup()
        val result = testBatch
                .map { it.outcome }
                .groupingBy { it }
                .eachCount()
                .map { (outcome, count) ->
                    OutcomeInfo(outcome, count, outcomeSetup?.getType(outcome))
                }
        return result
    }
}
