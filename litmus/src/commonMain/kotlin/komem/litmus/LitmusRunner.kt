package komem.litmus

import kotlin.time.Duration
import kotlin.time.TimeSource

abstract class LitmusRunner {
    abstract fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult

    // be extremely careful due to LTOutcome = Any?
    protected fun List<LitmusOutcome>.calcStats(outcomeSpec: LitmusOutcomeSpec): LitmusResult = this
        .groupingBy { it }
        .eachCount()
        .map { (outcome, count) ->
            LitmusOutcomeStats(outcome, count.toLong(), outcomeSpec.getType(outcome))
        }
}

fun <S> LitmusRunner.runTest(
    timeLimit: Duration,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult {
    val results = mutableListOf<LitmusResult>()
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
fun <S> LitmusRunner.runTestParallel(
    instances: Int,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult {
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

fun <S> LitmusRunner.runTestParallel(
    params: LitmusRunParams,
    test: LitmusTest<S>
): LitmusResult = runTestParallel(
    cpuCount() / test.threadCount,
    params,
    test
)

fun <S> LitmusRunner.runTestParallel(
    instances: Int,
    timeLimit: Duration,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult {
    val results = mutableListOf<LitmusResult>()
    val start = TimeSource.Monotonic.markNow()
    while (start.elapsedNow() < timeLimit) {
        results += runTestParallel(instances, params, test)
    }
    return results.mergeResults()
}

fun <S> LitmusRunner.runTestParallel(
    timeLimit: Duration,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult = runTestParallel(
    cpuCount() / test.threadCount,
    timeLimit,
    params,
    test
)
