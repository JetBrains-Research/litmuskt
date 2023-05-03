package barriers

import barrier.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.objcPtr

class CinteropBarrier(private val thread_count: Int) : Barrier {

    private val barrierStruct: CPointer<CSpinBarrier>? = create_barrier(thread_count)

    override fun wait() {
        barrier_wait(barrierStruct)
    }
}
