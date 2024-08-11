package org.jetbrains.litmuskt

import platform.posix.sleep
import kotlin.test.Test
import kotlin.test.assertEquals

class AffinityTest {

    @Test
    fun testBasic() {
        val manager = affinityManager ?: return

        val t = PthreadThreadlike()
        val future = t.start(Unit) {
            sleep(2u)
        }
        val cpus = setOf(0)
        manager.setAffinity(t, cpus)
        assertEquals(cpus, manager.getAffinity(t))

        val moreCpus = setOf(1, 3, 5, 7, 9)
        assertEquals(true, manager.setAffinityAndCheck(t, moreCpus))

        future.await()
    }

}
