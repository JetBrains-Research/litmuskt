package org.jetbrains.litmuskt

/**
 * A "thread-like" is a wrapper for something that can be used as a thread, for example, Worker or pthread API.
 * For now, returning a value from "threads" is not supported (as it is not currently needed).
 */
interface Threadlike {
    /**
     * Start running the function in a "thread". Returns a "future" handle that will block when called until
     * the "thread" has completed.
     *
     * This function should be only called once.
     *
     * Note: if [function] throws, the resulting "future" should have the thrown exception as a [Result].
     */
    fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture

    /**
     * Dispose of any resources the "thread" has allocated. Blocks until the resources are cleaned.
     */
    fun dispose()
}

/**
 * A future that blocks on calling [await]. If the function we are waiting for threw, the [Result] will contain it.
 */
fun interface BlockingFuture {
    fun await(): Result<Unit>
}
