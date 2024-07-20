package org.jetbrains.litmuskt

import kotlinx.cinterop.*
import kpthread.k_pthread_create
import kpthread.k_pthread_join
import kpthread.k_pthread_t_alloc
import kpthread.k_pthread_t_free
import platform.posix.errno

@OptIn(ExperimentalForeignApi::class)
class PthreadThreadlike : Threadlike {

    val pthreadPtr = k_pthread_t_alloc() ?: error("could not allocate pthread_t pointer")

    private class ThreadData<A : Any>(
        val args: A,
        val function: (A) -> Unit,
        var exception: Throwable?,
    )

    override fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture {
        val threadData = ThreadData(args, function, null)
        val threadDataRef = StableRef.create(threadData)

        k_pthread_create(
            pthreadPtr,
            staticCFunction<COpaquePointer?, COpaquePointer?> {
                val data = it!!.asStableRef<ThreadData<A>>().get()
                try {
                    data.function(data.args)
                } catch (e: Throwable) {
                    data.exception = e
                }
                println("pthread finish; exc=${data.exception}")
                return@staticCFunction null
            },
            threadDataRef.asCPointer()
        ).syscallCheck()

        return BlockingFuture {
            println("before join")
            println("errno=$errno")
            k_pthread_join(pthreadPtr, null).syscallCheck()
            println("pthread join")
            threadDataRef.dispose()
            threadData.exception?.let {
                Result.failure(it)
            } ?: Result.success(Unit)
        }
    }

    override fun dispose() {
        k_pthread_t_free(pthreadPtr)
    }
}
