package org.jetbrains.litmuskt

import kotlin.time.Duration

abstract class LitmusRunner {

    /**
     * Starts threads for the test and returns a "join handle". This handle should block
     * until the threads join and then collect and return the results.
     *
     * It should also reset() the states after running the test.
     */
    abstract fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?,
    ): BlockingFuture<LitmusResult>

    protected fun <S : Any> calcStats(
        states: Iterable<S>,
        spec: LitmusOutcomeSpec,
        outcomeFinalizer: (S) -> LitmusOutcome
    ): LitmusResult {
        // cannot do `map.getOrPut(key){0L}++` with Long-s, and by getting rid of one
        // extra put(), we are also getting rid of one extra hashCode()
        class LongHolder(var value: Long)

        // the absolute majority of outcomes will be declared in spec
        val specifiedOutcomes = (spec.accepted + spec.interesting + spec.forbidden).toTypedArray()
        val specifiedCounts = Array(specifiedOutcomes.size) { 0L }
        val useFastPath = specifiedOutcomes.size <= 10

        val totalCounts = mutableMapOf<LitmusOutcome, LongHolder>()

        for (s in states) {
            val outcome = outcomeFinalizer(s)
            if (useFastPath) {
                val i = specifiedOutcomes.indexOf(outcome)
                if (i != -1) {
                    specifiedCounts[i]++
                    continue
                }
            }
            totalCounts.getOrPut(outcome) { LongHolder(0L) }.value++
        }
        // update totalCounts with fastPathCounts
        for (i in specifiedCounts.indices) {
            val count = specifiedCounts[i]
            if (count > 0) totalCounts
                .getOrPut(specifiedOutcomes[i]) { LongHolder(0L) }
                .value = count
        }

        return totalCounts.map { (outcome, count) ->
            LitmusOutcomeStats(outcome, count.value, spec.getType(outcome))
        }
    }
}

/**
 * Runs [test] with [params], [timeLimit] and in parallel [instances].
 *
 * If [timeLimit] is not given, run the test once. If [instances] is not given, use as
 * many as possible without overlapping CPU cores between instances.
 *
 * Note: interprets AffinityMap as a sequence of smaller maps.
 * Example: for a map [ [0], [1], [2], [3] ], a test with 2 threads, and 2 instances, the
 * first instance will have a [ [0], [1] ] map and the second one will have [ [2], [3] ].
 */
fun <S : Any> LitmusRunner.runSingleTestParallel(
    test: LitmusTest<S>,
    params: LitmusRunParams,
    timeLimit: Duration = Duration.ZERO,
    instances: Int = cpuCount() / test.threadCount,
): LitmusResult {
    // allocate all states once, as it takes the most time compared to anything else
    val allStates = List(instances) {
        TypedArray(params.batchSize) { test.stateProducer() }
    }
    return repeatFor(timeLimit) {
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
        val allResults = allJoinHandles.map { it.await() }
        return@repeatFor allResults.mergeResults()
    }.mergeResults()
}

/**
 * Runs [tests] one by one, each with [params] and [timeLimit].
 *
 * If [timeLimit] is not given, run each test once.
 */
inline fun <reified S : Any> LitmusRunner.runTests(
    tests: List<LitmusTest<S>>,
    params: LitmusRunParams,
    timeLimit: Duration = Duration.ZERO,
): List<LitmusResult> = tests.map { test ->
    val states = TypedArray(params.batchSize) { test.stateProducer() }
    repeatFor(timeLimit) {
        startTest(
            test, states, params.barrierProducer, params.syncPeriod, params.affinityMap
        ).await()
    }.mergeResults()
}
