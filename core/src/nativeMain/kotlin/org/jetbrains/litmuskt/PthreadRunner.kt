package org.jetbrains.litmuskt

class PthreadRunner : ThreadlikeRunner() {
    override fun threadlikeProducer(): Threadlike = PthreadThreadlike()
}

//import kotlinx.cinterop.*
//import kpthread.k_pthread_create
//import kpthread.k_pthread_join
//import kpthread.k_pthread_t_alloc
//import kpthread.k_pthread_t_free
//
//private class ThreadData<S : Any>(
//    val states: Array<S>,
//    val function: (Any) -> Unit,
//    val syncPeriod: Int,
//    val barrier: Barrier,
//)
//
//private fun <S : Any> threadRoutine(data: ThreadData<S>): Unit = with(data) {
//    for (i in states.indices) {
//        function(states[i])
//        if (i % syncPeriod == 0) barrier.await()
//    }
//}
//
///**
// * A runner based on pthread API provided by C interop from stdlib.
// */
//class PthreadRunner : LitmusRunner() {
//
//    @OptIn(ExperimentalForeignApi::class)
//    override fun <S : Any> startTest(
//        test: LitmusTest<S>,
//        states: Array<S>,
//        barrierProducer: BarrierProducer,
//        syncPeriod: Int,
//        affinityMap: AffinityMap?
//    ): () -> LitmusResult {
//        val barrier = barrierProducer(test.threadCount)
//
//        fun startThread(threadIndex: Int): Pair<COpaquePointer, StableRef<*>> {
//            val function: (Any) -> Unit = { state ->
//                @Suppress("UNCHECKED_CAST")
//                test.threadFunctions[threadIndex].invoke(state as S)
//            }
//            val threadData = ThreadData(states, function, syncPeriod, barrier)
//            val threadDataRef = StableRef.create(threadData)
//
//            val pthreadPtr = k_pthread_t_alloc() ?: error("could not allocate pthread_t pointer")
//            k_pthread_create(
//                pthreadPtr,
//                staticCFunction<COpaquePointer?, COpaquePointer?> {
//                    val data = it!!.asStableRef<ThreadData<S>>().get()
//                    threadRoutine(data)
//                    return@staticCFunction null
//                },
//                threadDataRef.asCPointer()
//            ).syscallCheck()
//
//            // TODO: I don't think there is a way to assign affinity before the thread starts (would be useful for MacOS)
////            getAffinityManager()?.let { am ->
////                val map = affinityMap?.allowedCores(threadIndex) ?: return@let
////                am.setAffinity(pthreadVar.value, map)
////                require(am.getAffinity(pthreadVar.value) == map) { "setting affinity failed" }
////            }
//            return pthreadPtr to threadDataRef
//        }
//
//        val threads = List(test.threadCount) { startThread(it) }
//
//        return {
//            for ((pthreadPtr, threadDataRef) in threads) {
//                k_pthread_join(pthreadPtr, null).syscallCheck()
//                k_pthread_t_free(pthreadPtr)
//                threadDataRef.dispose()
//            }
//            calcStats(states, test.outcomeSpec, test.outcomeFinalizer)
//        }
//    }
//}
