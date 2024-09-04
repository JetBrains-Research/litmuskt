package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.barriers.CinteropSpinBarrier
import org.jetbrains.litmuskt.barriers.KNativeSpinBarrier
import platform.posix.sleep
import kotlin.concurrent.Volatile
import kotlin.test.Test
import kotlin.test.assertEquals

class BarrierTest {

    fun testBarrier(barrierProducer: (Int) -> Barrier) {
        val t1 = PthreadThreadlike()
        val t2 = PthreadThreadlike()

        class Context(
            val barrier: Barrier,
            var x: Int = 0,
            var y: Int = 0,
            @Volatile
            var flush: Int = 0 // ensure that changes are visible
        )

        val ctx = Context(barrierProducer(2))

        val f1 = t1.start(ctx) {
            it.barrier.await() // sync #1
            it.x = 1
            it.flush++

            sleep(2u)
            assertEquals(0, it.y) // thread 2 is waiting
            it.barrier.await() // sync #2
            sleep(1u)
            assertEquals(1, it.y) // thread 2 has continued
        }
        val f2 = t2.start(ctx) {
            sleep(1u)
            assertEquals(0, it.x) // thread 1 is waiting
            it.barrier.await() // sync #1
            sleep(1u)
            assertEquals(1, it.x) // thread 1 has continued

            it.barrier.await() // sync #2
            it.y = 1
            it.flush++
        }

        f1.await()
        f2.await()
    }

    @Test
    fun testCinteropSpinBarrier() = testBarrier { CinteropSpinBarrier(it) }

    @Test
    fun testKNativeSpinBarrier() = testBarrier { KNativeSpinBarrier(it) }

}
