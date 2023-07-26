package komem.litmus.barriers

import java.util.concurrent.CyclicBarrier

class JvmBarrier(threadCount: Int) : Barrier {
    private val barrier = CyclicBarrier(threadCount)

    override fun await() {
        barrier.await()
    }
}
