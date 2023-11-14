package komem.litmus

import komem.litmus.barriers.Barrier
import kotlinx.cinterop.*
import platform.posix.errno
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.strerror

class ThreadData(
    val states: List<Any?>,
    val function: (Any?) -> Unit,
    val syncPeriod: Int,
    val barrier: Barrier,
)

fun threadRoutine(data: ThreadData) = data.run {
    for (i in states.indices) {
        function(states[i])
        if (i % syncPeriod == 0) barrier.await()
    }
}

@OptIn(ExperimentalForeignApi::class)
private typealias PthreadVar = ULongVar

object PthreadRunner : LitmusRunner() {
    @OptIn(ExperimentalForeignApi::class)
    override fun <S> runTest(params: LitmusRunParams, test: LitmusTest<S>): LitmusResult = memScoped {

        val states = List(params.batchSize) { test.stateProducer() }
        val barrier = params.barrierProducer(test.threadCount)

        fun startThread(index: Int): Pair<PthreadVar, StableRef<*>> {
            val function: (Any?) -> Unit = { state ->
                // TODO: fix thread function signature
                @Suppress("UNCHECKED_CAST")
                test.threadFunctions[index].invoke(state as S)
            }
            val threadData = ThreadData(states, function, params.syncPeriod, barrier)

            val threadDataRef = StableRef.create(threadData)
            val pthreadHandleVar = alloc<PthreadVar>()
            val code = pthread_create(
                __newthread = pthreadHandleVar.ptr,
                __attr = null,
                __start_routine = staticCFunction<COpaquePointer?, COpaquePointer?> {
                    val data = it!!.asStableRef<ThreadData>().get()
                    threadRoutine(data)
                    return@staticCFunction null
                },
                __arg = threadDataRef.asCPointer(),
            )
            if (code != 0) error("pthread_create failed; errno=${strerror(errno)}")
            return pthreadHandleVar to threadDataRef
        }

        // TODO: affinity
        val (handleVars, refsToClear) = List(test.threadCount) { startThread(it) }.unzip()
        for (handleVar in handleVars) {
            pthread_join(handleVar.value, null)
        }

        for (ref in refsToClear) ref.dispose()
        val outcomes = states.map { test.outcomeFinalizer(it) }
        return@memScoped outcomes.calcStats(test.outcomeSpec)
    }
}
