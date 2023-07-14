package komem.litmus.runners

import komem.litmus.*
import kotlin.time.Duration
import kotlin.time.TimeSource

interface LitmusTestRunner {
    fun <S> runTest(params: RunParams, test: LTDefinition<S>): List<LitmusOutcome>
}

fun <S> LitmusTestRunner.runTest(
    timeLimit: Duration,
    params: RunParams,
    test: LTDefinition<S>,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTest(params, test)
        val outcomeSetup = test.outcomeSetup
        results.addAll(outcomes.groupIntoInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

/*
 * Note: interprets AffinityMap as a sequence of smaller maps
 * Example: for a map [ [0], [1], [2], [3] ],a test with 2 threads, and 2 instances, the
 * first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
 */
fun <S> LitmusTestRunner.runTestParallel(
    instances: Int,
    params: RunParams,
    test: LTDefinition<S>,
): List<LitmusOutcome> {
    val allOutcomes = List(instances) { instanceIndex ->
        val newAffinityMap = params.affinityMap?.let { oldMap ->
            AffinityMap { threadIndex ->
                oldMap.allowedCores(instanceIndex * test.threadCount + threadIndex)
            }
        }
        val newParams = params.copy(affinityMap = newAffinityMap)
        runTest(newParams, test)
    }
    return allOutcomes.flatten()
}

fun <S> LitmusTestRunner.runTestParallel(
    params: RunParams,
    test: LTDefinition<S>
): List<LitmusOutcome> = runTestParallel(
    cpuCount() / test.threadCount,
    params,
    test
)


fun <S> LitmusTestRunner.runTestParallel(
    instances: Int,
    timeLimit: Duration,
    params: RunParams,
    test: LTDefinition<S>,
): LitmusResult {
    val results = mutableListOf<LitmusOutcomeInfo>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTestParallel(instances, params, test)
        val outcomeSetup = test.outcomeSetup
        results.addAll(outcomes.groupIntoInfo(outcomeSetup))
    }
    return results.mergeOutcomes()
}

fun <S> LitmusTestRunner.runTestParallel(
    timeLimit: Duration,
    params: RunParams,
    test: LTDefinition<S>,
): LitmusResult = runTestParallel(
    cpuCount() / test.threadCount,
    timeLimit,
    params,
    test
)
