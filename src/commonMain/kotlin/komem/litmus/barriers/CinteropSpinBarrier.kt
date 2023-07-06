package komem.litmus.barriers

import barrier.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class CinteropSpinBarrier(private val thread_count: Int) : Barrier {

    private val barrierStruct: CPointer<CSpinBarrier>? = create_barrier(thread_count)

    override fun wait() {
        barrier_wait(barrierStruct)
    }
}
