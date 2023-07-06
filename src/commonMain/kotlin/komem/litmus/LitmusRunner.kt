package komem.litmus

import kotlin.system.getTimeMillis
import kotlin.time.Duration

interface LitmusRunner {

    fun runTest(options: LitmusOptions, testProducer: () -> LitmusTest): List<LitmusOutcome>

}

fun LitmusRunner.runTest(
    timeLimit: Duration,
    options: LitmusOptions,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<OutcomeInfo>()
    val start = getTimeMillis()
    while (getTimeMillis() - start < timeLimit.inWholeMilliseconds) {
        val outcomes = runTest(options, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.group(outcomeSetup))
    }
    return results.mergeOutcomes()
}

fun LitmusRunner.runTestParallel(
    timeLimit: Duration,
    options: LitmusOptions,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<OutcomeInfo>()
    val start = getTimeMillis()
    while (getTimeMillis() - start < timeLimit.inWholeMilliseconds) {
        val outcomes = runTestParallel(options, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.group(outcomeSetup))
    }
    return results.mergeOutcomes()
}

// TODO: generalize --- accept list of runners to run in parallel
//fun LitmusTestRunner.runTestParallel(
//    batchSize: Int,
//    parameters: LitmusTestParameters,
//    testProducer: () -> BasicLitmusTest,
//): LitmusResult {
//    BasicLitmusTest.memShuffler = parameters.memShufflerProducer?.invoke()
//    val threadFunctions: List<(BasicLitmusTest) -> Any?> = testProducer().overriddenthreads()
//
//    val parallelFthread = cpuCount() / threadFunctions.size
//    val testBatches = List(parallelFthread) { List(batchSize) { testProducer() } }
//    List(parallelFthread) { i ->
//        createFutures(
//            testBatches[i],
//            parameters.syncPeriod,
//            parameters.barrierProducer,
//            threadFunctions,
//            null
//        )
//    }.flatten().map { it.result }
//    val outcomes = testBatches
//        .flatten()
//        .onEach { it.arbiter() }
//        .let { collectOutcomes(it) }
//    return outcomes
//}