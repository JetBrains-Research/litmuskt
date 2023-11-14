package komem.litmus

import komem.litmus.barriers.Barrier
import kotlinx.cinterop.*
import platform.posix.*

private class ThreadData(
    val states: List<Any?>,
    val function: (Any?) -> Unit,
    val syncPeriod: Int,
    val barrier: Barrier,
)

private fun threadRoutine(data: ThreadData): Unit = with(data) {
    for (i in states.indices) {
        function(states[i])
        if (i % syncPeriod == 0) barrier.await()
    }
}

@OptIn(ExperimentalForeignApi::class)
// pthread_t = ULong
private typealias PthreadVar = ULongVar

object PthreadRunner : LitmusRunner() {
    @OptIn(ExperimentalForeignApi::class)
    override fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult = memScoped {
        pthread_t
        val states = List(params.batchSize) { test.stateProducer() }
        val barrier = params.barrierProducer(test.threadCount)

        fun startThread(threadIndex: Int): Pair<PthreadVar, StableRef<*>> {
            val function: (Any?) -> Unit = { state ->
                // TODO: fix thread function signature
                @Suppress("UNCHECKED_CAST")
                test.threadFunctions[threadIndex].invoke(state as S)
            }
            val threadData = ThreadData(states, function, params.syncPeriod, barrier)

            val threadDataRef = StableRef.create(threadData)
            val pthreadVar = alloc<PthreadVar>()
            val code = pthread_create(
                __newthread = pthreadVar.ptr,
                __attr = null,
                __start_routine = staticCFunction<COpaquePointer?, COpaquePointer?> {
                    val data = it!!.asStableRef<ThreadData>().get()
                    threadRoutine(data)
                    return@staticCFunction null
                },
                __arg = threadDataRef.asCPointer(),
            )
            if (code != 0) error("pthread_create failed; errno means: ${strerror(errno)?.toKString()}")
            // TODO: I don't think there is a way to assign affinity before the thread starts (would be useful for MacOS)
            getAffinityManager()?.let { am ->
                val map = params.affinityMap?.allowedCores(threadIndex) ?: return@let
                am.setAffinity(pthreadVar.value, map)
                require(am.getAffinity(pthreadVar.value) == map) { "setting affinity failed" }
            }
            return pthreadVar to threadDataRef
        }

        val (pthreadVars, refsToClear) = List(test.threadCount) { startThread(it) }.unzip()
        for (pthreadVar in pthreadVars) {
            pthread_join(pthreadVar.value, null)
        }

        for (ref in refsToClear) ref.dispose()
        val outcomes = states.map { test.outcomeFinalizer(it) }
        return@memScoped outcomes.calcStats(test.outcomeSpec)
    }
}
