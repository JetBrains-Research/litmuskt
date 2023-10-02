package komem.litmus

// does not support affinity
object JvmThreadRunner : LitmusRunner() {

    override fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult {

        val states = List(params.batchSize) { test.stateProducer() }
        val barrier = params.barrierProducer(test.threadCount)
        val outcomeFinalizer = test.outcomeFinalizer

        val threads = List(test.threadCount) { threadIndex ->
            Thread {
                val threadFunction = test.threadFunctions[threadIndex]
                val syncPeriod = params.syncPeriod
                for (i in states.indices) {
                    if (i % syncPeriod == 0) barrier.await()
                    states[i].threadFunction()
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() } // await all threads

        val outcomes = states.map { it.outcomeFinalizer() }
        assert(outcomes.size == params.batchSize)
        return outcomes.calcStats(test.outcomeSpec)
    }
}
