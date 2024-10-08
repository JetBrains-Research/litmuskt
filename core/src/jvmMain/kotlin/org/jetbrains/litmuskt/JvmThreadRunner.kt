package org.jetbrains.litmuskt

/**
 * A simplistic runner based on JVM threads. Does not support affinity.
 */
class JvmThreadRunner : LitmusRunner() {

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): BlockingFuture<LitmusResult> {
        val barrier = barrierProducer(test.threadCount)

        val threads = List(test.threadCount) { threadIndex ->
            Thread {
                val threadFunction = test.threadFunctions[threadIndex]
                for (i in states.indices) {
                    if (i % syncPeriod == 0) barrier.await()
                    states[i].threadFunction()
                }
            }
        }
        threads.forEach { it.start() }

        return BlockingFuture {
            threads.forEach { it.join() }
            calcStats(states.asIterable(), test.outcomeSpec, test.outcomeFinalizer)
        }
    }
}
