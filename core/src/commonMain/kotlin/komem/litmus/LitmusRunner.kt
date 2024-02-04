package komem.litmus

import kotlin.time.Duration
import kotlin.time.TimeSource

abstract class LitmusRunner {
    /**
     * Starts threads for the test and returns a "join handle". These handles should block
     * until the threads join and then collect and return the results. This is crucial for parallelism.
     */
    abstract fun <S : Any> startTest(params: LitmusRunParams, test: LitmusTest<S>): () -> LitmusResult

    /**
     * Runs the test. Blocks the current thread until the test finishes.
     */
    fun <S : Any> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult {
        return startTest(params, test)()
    }

    // be extremely careful due to LitmusOutcome = Any?
    protected fun List<LitmusOutcome>.calcStats(outcomeSpec: LitmusOutcomeSpec): LitmusResult = this
        .groupingBy { it }
        .eachCount()
        .map { (outcome, count) ->
            LitmusOutcomeStats(outcome, count.toLong(), outcomeSpec.getType(outcome))
        }
}

fun <S : Any> LitmusRunner.runTest(
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
fun <S : Any> LitmusRunner.runTestParallel(
    instances: Int,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult {
    val allJoinHandles = List(instances) { instanceIndex ->
        val newAffinityMap = params.affinityMap?.let { oldMap ->
            AffinityMap { threadIndex ->
                oldMap.allowedCores(instanceIndex * test.threadCount + threadIndex)
            }
        }
        val newParams = params.copy(affinityMap = newAffinityMap)
        startTest(newParams, test)
    }
    return allJoinHandles.map { it() }.mergeResults()
}

fun <S : Any> LitmusRunner.runTestParallel(
    params: LitmusRunParams,
    test: LitmusTest<S>
): LitmusResult = runTestParallel(
    cpuCount() / test.threadCount,
    params,
    test
)

fun <S : Any> LitmusRunner.runTestParallel(
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

fun <S : Any> LitmusRunner.runTestParallel(
    timeLimit: Duration,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult = runTestParallel(
    cpuCount() / test.threadCount,
    timeLimit,
    params,
    test
)
