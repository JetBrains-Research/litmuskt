package org.jetbrains.litmuskt

import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadlikeTest {

    private fun testThreadlike(t: Threadlike) {
        class IntHolder(var x: Int)

        val holder = IntHolder(5)
        val future = t.start(holder) {
            it.x = 7
        }
        future.await()
        assertEquals(7, holder.x)
    }

    @Test
    fun testWorkerThreadlike() = testThreadlike(WorkerThreadlike())

    @Test
    fun testPthreadThreadlike() = testThreadlike(PthreadThreadlike())
}
