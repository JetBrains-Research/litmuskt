package komem.litmus.runners

import komem.litmus.*
import kotlin.time.Duration
import kotlin.time.TimeSource

fun LitmusTestRunner.runTest(
    timeLimit: Duration,
    params: RunningParams,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTest(params, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.groupToInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

/*
 Note: interprets AffinityMap as a sequence of smaller maps
 Example: for a map [ [0], [1], [2], [3] ],a test with 2 threads, and 2 instances, the
 first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
 */
fun LitmusTestRunner.runTestParallel(
    params: RunningParams,
    instances: Int,
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
    params: RunningParams,
    testProducer: () -> LitmusTest,
): List<LitmusOutcome> = with(cpuCount() / testProducer().overriddenThreads().size) {
    runTestParallel(params, this, testProducer)
}

fun LitmusTestRunner.runTestParallel(
    timeLimit: Duration,
    params: RunningParams,
    instances: Int,
    testProducer: () -> LitmusTest,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTestParallel(params, instances, testProducer)
        val outcomeSetup = testProducer().getOutcomeSetup()
        results.addAll(outcomes.groupToInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

fun LitmusTestRunner.runTestParallel(
    timeLimit: Duration,
    params: RunningParams,
    testProducer: () -> LitmusTest,
): LitmusResult = with(cpuCount() / testProducer().overriddenThreads().size) {
    runTestParallel(timeLimit, params, this, testProducer)
}
