package komem.litmus.barriers

import java.util.concurrent.CyclicBarrier

class JvmCyclicBarrier(threadCount: Int) : Barrier {
    private val barrier = CyclicBarrier(threadCount)

    override fun await() {
        barrier.await()
    }
}
