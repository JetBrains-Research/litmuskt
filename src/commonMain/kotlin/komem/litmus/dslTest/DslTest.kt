@file:OptIn(ObsoleteWorkersApi::class)

package komem.litmus.dslTest

import komem.litmus.barriers.Barrier
import komem.litmus.barriers.SpinBarrier
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

interface LTContext<T> {
    fun thread(code: T.() -> Unit)
    var o1: Any?
    var o2: Any?

    fun init(producer: () -> T)
    fun after(code: T.() -> Unit)
}

class LTContainer<T> : LTContext<T> {
    val threadFs = mutableListOf<T.() -> Unit>()
    override var o1: Any? = null
    override var o2: Any? = null

    override fun thread(code: T.() -> Unit) {
        threadFs.add(code)
    }

    var afterF: (T.() -> Unit)? = null
    override fun after(code: T.() -> Unit) {
        afterF = code
    }

    val threadCount get() = threadFs.size

    lateinit var dataProducer: () -> T

    override fun init(producer: () -> T) {
        dataProducer = producer
    }
}

data class WorkerCtx<T>(
    val n: Int,
    val ownIndex: Int,
    val barrier: Barrier,
    val containers: List<LTContainer<T>>,
    val data: List<T>,
)

@OptIn(ObsoleteWorkersApi::class)
class LTDef<T>(
    private val def: LTContext<T>.() -> Unit
) {
    fun run(n: Int) {
        val infoContainer = LTContainer<T>().also(def)
        val containers = List(n) {
            LTContainer<T>().also { it.def() }
        }
        val data = List(n) {
            infoContainer.dataProducer()
        }
        val barrier = SpinBarrier(infoContainer.threadCount)
        val workers = infoContainer.threadFs.map { Worker.start() }
        val futures = workers.mapIndexed { wi, w ->
            val _ctx = WorkerCtx(n, wi, barrier, containers, data)
            w.execute(TransferMode.SAFE, { _ctx }) { ctx ->
                ctx.containers.forEachIndexed { ci, c ->
                    c.threadFs[ctx.ownIndex](ctx.data[ci])
                    if (ci % 10 == 0) {
                        ctx.barrier.wait()
                    }
                }
            }
        }
        for (w in workers) w.requestTermination(true)
        for (f in futures) f.result
        for ((c, d) in containers zip data) {
            c.afterF?.invoke(d)
        }
        val results = containers.map { it.o1 to it.o2 }.groupingBy { it }.eachCount()
        println(results)
    }
}

fun<T> litmusTest(def: LTContext<T>.() -> Unit): LTDef<T> = LTDef(def)
