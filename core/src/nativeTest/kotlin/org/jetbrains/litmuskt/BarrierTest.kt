package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.barriers.CinteropSpinBarrier
import org.jetbrains.litmuskt.barriers.KNativeSpinBarrier
import platform.posix.sleep
import kotlin.concurrent.Volatile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BarrierTest {

    fun testBarrier(barrierProducer: (Int) -> Barrier) {
        val t1 = PthreadThreadlike()
        val t2 = PthreadThreadlike()

        class Context(
            val barrier: Barrier,
            var x: Int = 0,
            @Volatile
            var flush: Int = 0 // ensure that changes are visible
        )

        val ctx = Context(barrierProducer(2))

        val f1 = t1.start(ctx) {
            it.barrier.await()
            it.x = 1
            it.flush = it.x + 1
        }
        val f2 = t2.start(ctx) {
            sleep(1u)
            assertNotEquals(1, it.x)
            it.barrier.await()
            sleep(1u)
            assertEquals(1, it.x)
        }

        f1.await()
        f2.await()
    }

    @Test
    fun testCinteropSpinBarrier() = testBarrier { CinteropSpinBarrier(it) }

    @Test
    fun testKNativeSpinBarrier() = testBarrier { KNativeSpinBarrier(it) }

}
