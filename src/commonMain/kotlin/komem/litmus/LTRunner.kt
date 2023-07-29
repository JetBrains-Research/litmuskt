package komem.litmus

import kotlin.time.Duration
import kotlin.time.TimeSource

interface LTRunner {
    fun <S> runTest(params: LTRunParams, test: LTDefinition<S>): List<LTOutcome>
}

fun <S> LTRunner.runTest(
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): List<LTOutcome> {
    val results = mutableListOf<LTOutcome>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTest(params, test)
        results.addAll(outcomes)
    }
    return results
}

/*
 * Note: interprets AffinityMap as a sequence of smaller maps
 * Example: for a map [ [0], [1], [2], [3] ],a test with 2 threads, and 2 instances, the
 * first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
 */
fun <S> LTRunner.runTestParallel(
    instances: Int,
    params: LTRunParams,
    test: LTDefinition<S>,
): List<LTOutcome> {
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

fun <S> LTRunner.runTestParallel(
    params: LTRunParams,
    test: LTDefinition<S>
): List<LTOutcome> = runTestParallel(
    cpuCount() / test.threadCount,
    params,
    test
)

fun <S> LTRunner.runTestParallel(
    instances: Int,
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): List<LTOutcome> {
    val results = mutableListOf<LTOutcome>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        val outcomes = runTestParallel(instances, params, test)
        results.addAll(outcomes)
    }
    return results
}

fun <S> LTRunner.runTestParallel(
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): List<LTOutcome> = runTestParallel(
    cpuCount() / test.threadCount,
    timeLimit,
    params,
    test
)
