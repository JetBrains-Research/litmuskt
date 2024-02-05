package komem.litmus

import kotlin.time.Duration
import kotlin.time.TimeSource

abstract class LitmusRunner {

    /**
     * Starts threads for the test and returns a "join handle". This handle should block
     * until the threads join and then collect and return the results.
     */
    protected abstract fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: List<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?,
    ): () -> LitmusResult

    /**
     * Entry point for running tests. This method can be overridden in case that particular runner
     * does not need to allocate states.
     */
    open fun <S : Any> startTest(params: LitmusRunParams, test: LitmusTest<S>): () -> LitmusResult {
        val states = List(params.batchSize) { test.stateProducer() }
        return startTest(test, states, params.barrierProducer, params.syncPeriod, params.affinityMap)
    }

    /**
     * Entry point for running tests in parallel. Again, can be overridden in case a particular runner
     * implements parallel runs in a different manner.
     *
     * Note: default implementation interprets AffinityMap as a sequence of smaller maps.
     * Example: for a map [ [0], [1], [2], [3] ],a test with 2 threads, and 2 instances, the
     * first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
     */
    open fun <S : Any> LitmusRunner.startTestParallel(
        instances: Int,
        params: LitmusRunParams,
        test: LitmusTest<S>,
    ): List<() -> LitmusResult> {
        // separated due to allocations severely impacting threads
        val allStates = List(instances) {
            List(params.batchSize) { test.stateProducer() }
        }
        val allJoinHandles = List(instances) { instanceIndex ->
            val newAffinityMap = params.affinityMap?.let { oldMap ->
                AffinityMap { threadIndex ->
                    oldMap.allowedCores(instanceIndex * test.threadCount + threadIndex)
                }
            }
            startTest(
                test = test,
                states = allStates[instanceIndex],
                barrierProducer = params.barrierProducer,
                syncPeriod = params.syncPeriod,
                affinityMap = newAffinityMap,
            )
        }
        return allJoinHandles
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
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult = startTest(params, test).invoke()

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

fun <S : Any> LitmusRunner.runTestParallel(
    instances: Int,
    params: LitmusRunParams,
    test: LitmusTest<S>,
): LitmusResult = startTestParallel(instances, params, test).map { it() }.mergeResults()

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
