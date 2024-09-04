package org.jetbrains.litmuskt

import platform.posix.sleep
import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadlikeTest {

    private fun testThreadlike(t: Threadlike) {
        class IntHolder(var x: Int)

        val holder = IntHolder(0)
        val future = t.start(holder) {
            sleep(1u)
            it.x = 1
        }
        assertEquals(0, holder.x) // checking parallelism
        future.await()
        assertEquals(1, holder.x)
    }

    @Test
    fun testWorkerThreadlike() = testThreadlike(WorkerThreadlike())

    @Test
    fun testPthreadThreadlike() = testThreadlike(PthreadThreadlike())
}
