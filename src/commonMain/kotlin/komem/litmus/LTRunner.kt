package komem.litmus

import kotlin.time.Duration
import kotlin.time.TimeSource

abstract class LTRunner {
    abstract fun <S> runTest(params: LTRunParams, test: LTDefinition<S>): LTResult

    // be extremely careful due to LTOutcome = Any?
    protected fun List<LTOutcome>.calcStats(outcomeSpec: LTOutcomeSpec): LTResult = this
        .groupingBy { it }
        .eachCount()
        .map { (outcome, count) ->
            LTOutcomeStats(outcome, count.toLong(), outcomeSpec.getType(outcome))
        }
}

fun <S> LTRunner.runTest(
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): LTResult {
    val results = mutableListOf<LTResult>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        results += runTest(params, test)
    }
    return results.mergeResults()
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
): LTResult {
    val allOutcomes = List(instances) { instanceIndex ->
        val newAffinityMap = params.affinityMap?.let { oldMap ->
            AffinityMap { threadIndex ->
                oldMap.allowedCores(instanceIndex * test.threadCount + threadIndex)
            }
        }
        val newParams = params.copy(affinityMap = newAffinityMap)
        runTest(newParams, test)
    }
    return allOutcomes.mergeResults()
}

fun <S> LTRunner.runTestParallel(
    params: LTRunParams,
    test: LTDefinition<S>
): LTResult = runTestParallel(
    cpuCount() / test.threadCount,
    params,
    test
)

fun <S> LTRunner.runTestParallel(
    instances: Int,
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): LTResult {
    val results = mutableListOf<LTResult>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        results += runTestParallel(instances, params, test)
    }
    return results.mergeResults()
}

fun <S> LTRunner.runTestParallel(
    timeLimit: Duration,
    params: LTRunParams,
    test: LTDefinition<S>,
): LTResult = runTestParallel(
    cpuCount() / test.threadCount,
    timeLimit,
    params,
    test
)