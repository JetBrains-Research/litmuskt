package komem.litmus.runners

import komem.litmus.*
import kotlin.time.Duration
import kotlin.time.TimeSource

interface LitmusTestRunner {
    fun runTest(params: RunParams, testProducer: () -> LitmusTest): List<LitmusOutcome>
}

fun LitmusTestRunner.runTest(
    timeLimit: Duration,
    params: RunParams,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTest(params, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.groupIntoInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

/*
 * Note: interprets AffinityMap as a sequence of smaller maps
 * Example: for a map [ [0], [1], [2], [3] ],a test with 2 threads, and 2 instances, the
 * first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
 */
fun LitmusTestRunner.runTestParallel(
    instances: Int,
    params: RunParams,
    testProducer: () -> LitmusTest,
): List<LitmusOutcome> {
    val threadCount = testProducer().overriddenThreads().size
    val allOutcomes = List(instances) { instanceIndex ->
        val newAffinityMap = params.affinityMap?.let { oldMap ->
            AffinityMap { threadIndex ->
                oldMap.allowedCores(instanceIndex * threadCount + threadIndex)
            }
        }
        val newParams = params.copy(affinityMap = newAffinityMap)
        runTest(newParams, testProducer)
    }
    return allOutcomes.flatten()
}

fun LitmusTestRunner.runTestParallel(
    params: RunParams,
    testProducer: () -> LitmusTest,
): List<LitmusOutcome> = runTestParallel(
    cpuCount() / testProducer().overriddenThreads().size,
    params,
    testProducer
)


fun LitmusTestRunner.runTestParallel(
    instances: Int,
    timeLimit: Duration,
    params: RunParams,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTestParallel(instances, params, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.groupIntoInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

fun LitmusTestRunner.runTestParallel(
    timeLimit: Duration,
    params: RunParams,
    testProducer: () -> LitmusTest,
): LitmusResult = runTestParallel(
    cpuCount() / testProducer().overriddenThreads().size,
    timeLimit,
    params,
    testProducer
)
