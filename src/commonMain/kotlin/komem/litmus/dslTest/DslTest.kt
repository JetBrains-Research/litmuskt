@file:OptIn(ObsoleteWorkersApi::class)

package komem.litmus.dslTest

import komem.litmus.barriers.Barrier
import komem.litmus.barriers.SpinBarrier
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

interface LTContext {
    fun thread(code: () -> Unit)
    var o1: Any?
    var o2: Any?
}

class LTContainer : LTContext {
    val threadFs = mutableListOf<() -> Unit>()
    override var o1: Any? = null
    override var o2: Any? = null

    override fun thread(code: () -> Unit) {
        threadFs.add(code)
    }

    val threadCount get() = threadFs.size
}

data class WorkerCtx(
    val n: Int,
    val ownIndex: Int,
    val barrier: Barrier,
    val containers: List<LTContainer>,
)

@OptIn(ObsoleteWorkersApi::class)
class LTDef(
    private val def: LTContext.() -> Unit
) {
    fun run(n: Int) {
        val containers = List(n) {
            LTContainer().also { it.def() }
        }
        val infoContainer = LTContainer().also(def)
        val barrier = SpinBarrier(infoContainer.threadCount)
        val workers = infoContainer.threadFs.map { Worker.start() }
        val futures = workers.mapIndexed { wi, w ->
            val _ctx = WorkerCtx(n, wi, barrier, containers)
            w.execute(TransferMode.SAFE, { _ctx }) { ctx ->
                ctx.containers.forEachIndexed { ci, c ->
                    c.threadFs[ctx.ownIndex]()
                    if (ci % 10 == 0) {
                        ctx.barrier.wait()
                    }
                }
            }
        }
        for (w in workers) w.requestTermination(true)
        for (f in futures) f.result
        val results = containers.map { it.o1 to it.o2 }.groupingBy { it }.eachCount()
        println(results)
    }
}

fun litmusTest(def: LTContext.() -> Unit): LTDef = LTDef(def)
