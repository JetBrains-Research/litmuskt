package org.jetbrains.litmuskt

import platform.posix.sleep
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

@OptIn(ObsoleteWorkersApi::class)
class WorkerThreadlike : Threadlike {

    val worker = Worker.start()

    private data class WorkerContext<A>(
        val args: A,
        val threadFunction: (A) -> Unit,
    )

    override fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture {
        val context = WorkerContext(args, function)
        val future: Future<Result<Unit>> = worker.execute(
            TransferMode.SAFE /* ignored */,
            { context }
        ) { ctx ->
            // Unit as a receiver so that runCatching() does not capture <this>
            Unit.runCatching {
                ctx.threadFunction(ctx.args)
            }.also {
                println("worker f end; result=$it")
            }
        }
        return BlockingFuture {
            println("entered blockingfuture")
            future.result.also {
                println("worker future awaited")
            }
        }
    }

    override fun dispose() {
        worker.requestTermination().result
    }
}
